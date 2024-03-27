package com.example.performancecache.service;

import com.example.performancecache.dto.Notice;
import com.example.performancecache.mapper.NoticeReadMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
public class NoticeServiceImpl implements NoticeService{

    private NoticeReadMapper noticeReadMapper;

    public NoticeServiceImpl(NoticeReadMapper noticeReadMapper){
        this.noticeReadMapper = noticeReadMapper;
    }

    @Override
    @Cacheable(value = "NoticeReadMapper.findAll")
    public List<Notice> getAllNotices() {
        return noticeReadMapper.findAll();
    }

    /*
     * key = "#request.requestURI + '-' + #pageNumber" : 요청 URI와 페이지 번호를 조합한 키로 캐시를 구분
     * condition = "#pageNumber <= 5" : 페이지 번호가 5 이하일 때만 캐시를 사용
     */
    @Override
    @Cacheable(value = "NoticeReadMapper.findByPage", key = "#request.requestURI + '-' + #pageNumber", condition = "#pageNumber <= 5")
    public List<Notice> findByPage(HttpServletRequest request, int pageNumber) {
        int startIdx = (pageNumber - 1) * 10;
        return noticeReadMapper.findByPage(startIdx);
    }

    @Override
    public List<Notice> findNoticesByDates(LocalDateTime startDate, LocalDateTime endDate) {
        return noticeReadMapper.findNoticesByDates(startDate, endDate);
    }
}
