package cn.yhl.kvstore2pcsystem.moudle_participant;

import cn.yhl.kvstore2pcsystem.Config;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class participantCondition implements Condition {
    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        return Config.getConfig().getMode()==1;
    }
}
