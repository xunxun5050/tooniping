package com.webtoonhub.webtoon.service;

import com.webtoonhub.common.exception.NotFoundException;
import com.webtoonhub.webtoon.dto.HomeResponseDto;
import com.webtoonhub.webtoon.dto.PagedResultDto;
import com.webtoonhub.webtoon.dto.WebtoonDetailDto;
import com.webtoonhub.webtoon.dto.WebtoonFiltersDto;
import com.webtoonhub.webtoon.dto.WebtoonListItemDto;
import com.webtoonhub.webtoon.dto.WebtoonSearchCondition;
import com.webtoonhub.webtoon.repository.WebtoonQueryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WebtoonService {

    private final WebtoonQueryRepository webtoonQueryRepository;

    public WebtoonService(WebtoonQueryRepository webtoonQueryRepository) {
        this.webtoonQueryRepository = webtoonQueryRepository;
    }

    public PagedResultDto<WebtoonListItemDto> getWebtoons(WebtoonSearchCondition condition) {
        return webtoonQueryRepository.findWebtoons(condition);
    }

    public WebtoonDetailDto getWebtoonDetail(long webtoonId) {
        return webtoonQueryRepository.findWebtoonDetail(webtoonId)
            .orElseThrow(() -> new NotFoundException("웹툰을 찾을 수 없습니다."));
    }

    public List<WebtoonListItemDto> getSimilarWebtoons(long webtoonId, int size) {
        webtoonQueryRepository.findWebtoonDetail(webtoonId)
            .orElseThrow(() -> new NotFoundException("웹툰을 찾을 수 없습니다."));
        return webtoonQueryRepository.findSimilarWebtoons(webtoonId, size);
    }

    public HomeResponseDto getHomeData() {
        return webtoonQueryRepository.findHomeData();
    }

    public WebtoonFiltersDto getFilters() {
        return webtoonQueryRepository.findFilters();
    }
}
