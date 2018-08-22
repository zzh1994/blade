package com.blade.kit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.*;
import java.util.Date;
import java.util.Locale;

/**
 * @author biezhi
 * @date 2017/9/20
 */
public class DateKitTest {

    private long          time;
    private Date          date;
    private LocalDateTime localDateTime;

    @Before
    public void before() {
        time = 1505892010L;
        date = new Date(1505892470110L);
        localDateTime = LocalDateTime.from(Instant.ofEpochSecond(time).atZone(ZoneId.systemDefault()));
    }

    @Test
    public void testToString() {
        String dateStr = DateKit.toString(date, "yyyy-MM-dd");
        Assert.assertEquals("2017-09-20", dateStr);
    }

    @Test
    public void testToString2() {
        String dateStr = DateKit.toString(time, "yyyy-MM-dd");
        Assert.assertEquals("2017-09-20", dateStr);
    }

    @Test
    public void testToString3() {
        String dateStr = DateKit.toString(localDateTime, "yyyy-MM-dd");
        String timeStr = DateKit.toString(localDateTime, "yyyy-MM-dd HH:mm:ss");
        Assert.assertEquals("2017-09-20", dateStr);
        Assert.assertEquals("2017-09-20 15:20:10", timeStr);
    }

    @Test
    public void testGmtDate() {
        String gmt = DateKit.gmtDate(date);
        Assert.assertEquals("Wed, 20 Sep 2017 15:27:50 GMT", gmt);

        gmt = DateKit.gmtDate(localDateTime);
        Assert.assertEquals("Wed, 20 Sep 2017 15:20:10 GMT", gmt);

        Assert.assertNotNull(DateKit.gmtDate());
    }

    @Test
    public void testToDateTime() {
        Date date = DateKit.toDate("2017-09-09", "yyyy-MM-dd");
        Assert.assertNotNull(date);

        date = DateKit.toDate(time);
        Assert.assertNotNull(date);

        Date dateTime = DateKit.toDateTime("2017-09-09 11:22:33", "yyyy-MM-dd HH:mm:ss");
        Assert.assertNotNull(dateTime);

        LocalDate localDate = DateKit.toLocalDate("2017-09-09", "yyyy-MM-dd");
        Assert.assertNotNull(localDate);

        LocalDateTime localDateTime = DateKit.toLocalDateTime("2017-09-09 11:22:33", "yyyy-MM-dd HH:mm:ss");
        Assert.assertNotNull(localDateTime);
    }

    @Test
    public void testToUnix() {
        int unixTime = DateKit.toUnix(date);
        Assert.assertEquals(1505892470, unixTime);

        unixTime = DateKit.toUnix("2017-09-09 11:22:33");
        Assert.assertEquals(1504927353, unixTime);

        unixTime = DateKit.toUnix("2017-09-09 11:22", "yyyy-MM-dd HH:mm");
        Assert.assertEquals(1504927320, unixTime);

        unixTime = DateKit.nowUnix();
        Assert.assertNotNull(unixTime);
    }

    @Test
    public void testPrettyTime() {
        Assert.assertEquals("去年", DateKit.prettyTime(LocalDateTime.now().plusYears(-1), Locale.CHINESE));
        Assert.assertEquals("上个月", DateKit.prettyTime(LocalDateTime.now().plusMonths(-1), Locale.CHINESE));
        Assert.assertEquals("上周", DateKit.prettyTime(LocalDateTime.now().plusWeeks(-1), Locale.CHINESE));
        Assert.assertEquals("昨天", DateKit.prettyTime(LocalDateTime.now().plusDays(-1), Locale.CHINESE));
        Assert.assertEquals("1小时前", DateKit.prettyTime(LocalDateTime.now().plusHours(-1), Locale.CHINESE));
        Assert.assertEquals("1分钟前", DateKit.prettyTime(LocalDateTime.now().plusMinutes(-1), Locale.CHINESE));
        Assert.assertEquals("刚刚", DateKit.prettyTime(LocalDateTime.now().plusSeconds(-1), Locale.CHINESE));
        Assert.assertEquals("10秒前", DateKit.prettyTime(LocalDateTime.now().plusSeconds(-10), Locale.CHINESE));
    }

}
