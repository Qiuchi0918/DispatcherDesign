package com.example.demo.conditionexpr;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ForEachExpression {
    public ForEachExpression(String expr) {
        this.expr = expr;
    }

    private Map<String, Object> paramNameValueMap;
    private final String expr;
    private Object paramToBeUsedInLoop;
    private Iterator<?> curIterPtr;
    public boolean initialized = false;

    /**
     * 设置表达式参数
     *
     * @param paramNameValueMap 表达式参数Map<参数名称,参数值>
     */
    private void setParameter(Map<String, Object> paramNameValueMap) {
        this.paramNameValueMap = paramNameValueMap;
    }

    public void initialize(Map<String, Object> paramNameValueMap) {
        setParameter(paramNameValueMap);
        execute();
        if (paramToBeUsedInLoop instanceof Map) {
            curIterPtr = ((Map<?, ?>) paramToBeUsedInLoop).entrySet().iterator();
        } else if (paramToBeUsedInLoop instanceof List) {
            curIterPtr = ((List<?>) paramToBeUsedInLoop).iterator();
        } else if (paramToBeUsedInLoop instanceof HashSet) {
            curIterPtr = ((HashSet<?>) paramToBeUsedInLoop).iterator();
        }
        initialized = true;
    }

    /**
     * 从参数中取回需要遍历的列表
     */
    private void execute() {
        Expression compiledExp = AviatorEvaluator.compile(expr);
        Object[] params = new Object[paramNameValueMap.size() * 2];
        int index = -1;
        for (Map.Entry<String, Object> curEntry : paramNameValueMap.entrySet()) {
            params[++index] = curEntry.getKey();
            params[++index] = curEntry.getValue();
        }
        paramToBeUsedInLoop = compiledExp.execute(compiledExp.newEnv(params));
    }

    /**
     * 取出当前循环的变量
     *
     * @return 当前循环变量
     */
    public Object getValueForNewIteration() {
        return curIterPtr.next();
    }

    public boolean hasNext() {
        return curIterPtr.hasNext();
    }
}
