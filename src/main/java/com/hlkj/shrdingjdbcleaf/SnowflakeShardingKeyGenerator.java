package com.hlkj.shrdingjdbcleaf;/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.shardingsphere.elasticjob.infra.env.TimeService;

import java.util.Calendar;
import java.util.Properties;
 
/**
 * Snowflake distributed primary key generator.
 * 
 * <p>
 * Use snowflake algorithm. Length is 64 bit.
 * </p>
 * 
 * <pre>
 * 1bit sign bit.
 * 41bits timestamp offset from 2016.11.01(ShardingSphere distributed primary key published data) to now.
 * 10bits worker process id.
 * 12bits auto increment offset in one mills
 * </pre>
 * 
 * <p>
 * Call @{@code SnowflakeShardingKeyGenerator.setWorkerId} to set worker id, default value is 0.
 * </p>
 * 
 * <p>
 * Call @{@code SnowflakeShardingKeyGenerator.setMaxTolerateTimeDifferenceMilliseconds} to set max tolerate time difference milliseconds, default value is 0.
 * </p>
 * 
 * 1     符号位             等于 0
 * 41    时间戳             从 2016/11/01 零点开始的毫秒数，支持 2 ^41 /365/24/60/60/1000=69.7年
 * 10    工作进程编号        支持 1024 个进程
 * 12    ***             每毫秒从 0 开始自增，支持 4096 个编号
 * 
 * @author gaohongtao
 * @author panjuan
 */
public final class SnowflakeShardingKeyGenerator {
    /**
     * 起始时间的毫秒（千分之一秒）数 
     */
    public static final long EPOCH;
    /**
     * 自增序列的bit位数（一个二进制数据0或1，是1bit） 
     */
    private static final long SEQUENCE_BITS = 12L;
    /**
     * 工作机器ID的bit位数（一个二进制数据0或1，是1bit） 
     */
    private static final long WORKER_ID_BITS = 10L;
    /**
     * 自增序列的掩码：4095，防止溢出
     * << 左移，不分正负数，低位补0
     * 那么1 << 12L 即二进制1右边补12个0，结果1000000000000，转为十进制是4096，相当于1乘以2的12次方
     */
    private static final long SEQUENCE_MASK = (1 << SEQUENCE_BITS) - 1;
    /**
     * 工作机器ID左移bit位数：自增序列的位数
     */
    private static final long WORKER_ID_LEFT_SHIFT_BITS = SEQUENCE_BITS;
    /**
     * 时间差左移bit位数：工作机器ID左移bit位数+工作机器ID的bit位数
     */
    private static final long TIMESTAMP_LEFT_SHIFT_BITS = WORKER_ID_LEFT_SHIFT_BITS + WORKER_ID_BITS;
    /**
     * 工作机器ID最大值：1<<10即10000000000，转十进制即1*2的10次方=1024
     */
    private static final long WORKER_ID_MAX_VALUE = 1L << WORKER_ID_BITS;
    /**
     * 工作机器ID默认值0 
     */
    private static final long WORKER_ID = 0;
    /**
     * 最大容忍时间差毫秒数 
     */
    private static final int MAX_TOLERATE_TIME_DIFFERENCE_MILLISECONDS = 10;
    
    @Setter
    private static TimeService timeService = new TimeService();
    
    @Getter
    @Setter
    private Properties properties = new Properties();
    
    private byte sequenceOffset;
    
    private long sequence;
    
    private long lastMilliseconds;
    
    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, Calendar.NOVEMBER, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        EPOCH = calendar.getTimeInMillis();
    }
    
    public String getType() {
        return "SNOWFLAKE";
    }
    
    public synchronized Comparable<?> generateKey() {
    	/**
    	 * 当前系统时间毫秒数 
    	 */ 
        long currentMilliseconds = timeService.getCurrentMillis();
        /**
         * 判断是否需要等待容忍时间差，如果需要，则等待时间差过去，然后再获取当前系统时间 
         */ 
        if (waitTolerateTimeDifferenceIfNeed(currentMilliseconds)) {
            currentMilliseconds = timeService.getCurrentMillis();
        }
        /**
         * 如果最后一次毫秒与 当前系统时间毫秒相同，即还在同一毫秒内 
         */
        if (lastMilliseconds == currentMilliseconds) {
        	/**
        	 * &位与运算符：两个数都转为二进制，如果相对应位都是1，则结果为1，否则为0
        	 * 当序列为4095时，4095+1后的新序列与掩码进行位与运算结果是0
        	 * 当序列为其他值时，位与运算结果都不会是0
        	 * 即本毫秒的序列已经用到最大值4096，此时要取下一个毫秒时间值
        	 */
            if (0L == (sequence = (sequence + 1) & SEQUENCE_MASK)) {
                currentMilliseconds = waitUntilNextTime(currentMilliseconds);
            }
        } else {
        	/**
        	 * 上一毫秒已经过去，把序列值重置为1 
        	 */
            vibrateSequenceOffset();
            sequence = sequenceOffset;
        }
        lastMilliseconds = currentMilliseconds;
        
        /**
         * XX......XX XX000000 00000000 00000000	时间差 XX
         *  		XXXXXX XXXX0000 00000000	机器ID XX
         *  		           XXXX XXXXXXXX	*** XX
         *  三部分进行|位或运算：如果相对应位都是0，则结果为0，否则为1
         */
        return ((currentMilliseconds - EPOCH) << TIMESTAMP_LEFT_SHIFT_BITS) | (getWorkerId() << WORKER_ID_LEFT_SHIFT_BITS) | sequence;
    }
    
    /**
     * 判断是否需要等待容忍时间差
     */
    @SneakyThrows
    private boolean waitTolerateTimeDifferenceIfNeed(final long currentMilliseconds) {
    	/**
    	 * 如果获取ID时的最后一次时间毫秒数小于等于当前系统时间毫秒数，属于正常情况，则不需要等待 
    	 */
        if (lastMilliseconds <= currentMilliseconds) {
            return false;
        }
        /**
         * ===>时钟回拨的情况（生成序列的时间大于当前系统的时间），需要等待时间差 
         */
        /**
         * 获取ID时的最后一次毫秒数减去当前系统时间毫秒数的时间差 
         */
        long timeDifferenceMilliseconds = lastMilliseconds - currentMilliseconds;
        /**
         * 时间差小于最大容忍时间差，即当前还在时钟回拨的时间差之内 
         */
        Preconditions.checkState(timeDifferenceMilliseconds < getMaxTolerateTimeDifferenceMilliseconds(), 
                "Clock is moving backwards, last time is %d milliseconds, current time is %d milliseconds", lastMilliseconds, currentMilliseconds);
        /**
         * 线程休眠时间差 
         */
        Thread.sleep(timeDifferenceMilliseconds);
        return true;
    }
    
    private long getWorkerId() {
        long result = Long.valueOf(properties.getProperty("worker.id", String.valueOf(WORKER_ID)));
        Preconditions.checkArgument(result >= 0L && result < WORKER_ID_MAX_VALUE);
        return result;
    }
    
    private int getMaxTolerateTimeDifferenceMilliseconds() {
        return Integer.valueOf(properties.getProperty("max.tolerate.time.difference.milliseconds", String.valueOf(MAX_TOLERATE_TIME_DIFFERENCE_MILLISECONDS)));
    }
    
    private long waitUntilNextTime(final long lastTime) {
        long result = timeService.getCurrentMillis();
        while (result <= lastTime) {
            result = timeService.getCurrentMillis();
        }
        return result;
    }
    
    /**
     * 把序列值重置为1
     */
    private void vibrateSequenceOffset() {
    	/**
    	 * byte是8位二进制
    	 * sequenceOffset默认值是0000 0000
    	 * ~sequenceOffset取反运算后是1111 1111
    	 * &1 位与运算后是0000 0001，转换为十进制就是1
    	 */
        sequenceOffset = (byte) (~sequenceOffset & 1);
    }
}