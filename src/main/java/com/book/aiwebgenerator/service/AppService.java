package com.book.aiwebgenerator.service;

import com.book.aiwebgenerator.model.dto.app.AppQueryRequest;
import com.book.aiwebgenerator.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.book.aiwebgenerator.model.entity.App;

import java.util.List;


public interface AppService extends IService<App> {
    AppVO getAppVO(App app);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    List<AppVO> getAppVOList(List<App> appList);
}
