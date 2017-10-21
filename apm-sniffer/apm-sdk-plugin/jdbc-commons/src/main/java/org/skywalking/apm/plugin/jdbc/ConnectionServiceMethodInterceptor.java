/*
 * Copyright 2017, OpenSkywalking Organization All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project repository: https://github.com/OpenSkywalking/skywalking
 */

package org.skywalking.apm.plugin.jdbc;

import java.lang.reflect.Method;
import org.skywalking.apm.agent.core.context.ContextManager;
import org.skywalking.apm.agent.core.context.tag.Tags;
import org.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;
import org.skywalking.apm.util.StringUtil;

/**
 * {@link ConnectionServiceMethodInterceptor} create an exit span when the client call the following methods in the class
 * that extend {@link java.sql.Connection}.
 * 1. close
 * 2. rollback
 * 3. releaseSavepoint
 * 4. commit
 * @author zhangxin
 */
public class ConnectionServiceMethodInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public final void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes,
        MethodInterceptResult result) throws Throwable {
        ConnectionInfo connectInfo = (ConnectionInfo)objInst.getSkyWalkingDynamicField();
        String remotePeer;
        if (!StringUtil.isEmpty(connectInfo.getHosts())) {
            remotePeer = connectInfo.getHosts();
        } else {
            remotePeer = connectInfo.getHost() + ":" + connectInfo.getPort();
        }
        AbstractSpan span = ContextManager.createExitSpan(connectInfo.getDBType() + "/JDBI/Connection/" + method.getName(), remotePeer);
        Tags.DB_TYPE.set(span, "sql");
        Tags.DB_INSTANCE.set(span, connectInfo.getDatabaseName());
        Tags.DB_STATEMENT.set(span, "");
        span.setComponent(connectInfo.getComponent());
        SpanLayer.asDB(span);
    }

    @Override
    public final Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes,
        Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override public final void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }

}
