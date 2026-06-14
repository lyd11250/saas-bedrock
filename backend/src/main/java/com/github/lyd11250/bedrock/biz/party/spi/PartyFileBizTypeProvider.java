package com.github.lyd11250.bedrock.biz.party.spi;

import com.github.lyd11250.bedrock.system.spi.FileBizTypeDef;
import com.github.lyd11250.bedrock.system.spi.FileBizTypeProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * party 模块自报旗下文件业务类型，兼作 biz 模块接入 {@link FileBizTypeProvider} 的样板：
 * 基座完全不知道这里的标签，仅在运行期聚合本 {@code @Component}。
 */
@Component
public class PartyFileBizTypeProvider implements FileBizTypeProvider {

    @Override
    public List<FileBizTypeDef> bizTypes() {
        return List.of(
                new FileBizTypeDef("party:person:idcard", "人员身份证"),
                new FileBizTypeDef("party:organization:qualification", "单位资质证书"),
                new FileBizTypeDef("party:person:qualification", "人员资质证书")
        );
    }
}
