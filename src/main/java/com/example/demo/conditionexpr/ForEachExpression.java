package com.example.demo.conditionexpr;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;

import java.util.List;
import java.util.Map;

public class ForEachExpression {
    public ForEachExpression(String expr) {
        this.expr = expr;
    }

    private Map<String, Object> paramNameValueMap;
    private final String expr;
    private List<Object> paramList;
    private int iterationIndex = 0;
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
        iterationIndex = 0;
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
        paramList = (List<Object>) compiledExp.execute(compiledExp.newEnv(params));
    }

    /**
     * 取出当前循环的变量
     *
     * @return 当前循环变量
     */
    public Object getValueForNewIteration() {
        return paramList.get(iterationIndex++);
    }

    public boolean hasNext() {
        return iterationIndex < paramList.size();
    }
}
