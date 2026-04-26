package com.daizhiyong.common.constants;

/**
 * API 响应常量
 * 统一定义所有微服务共用的 HTTP 状态码和响应消息
 */
public final class ApiResponseConstants {

    private ApiResponseConstants() {
        // 防止实例化
    }

    /**
     * 成功创建资源
     */
    public static final String STATUS_201 = "201";
    public static final String MESSAGE_201 = "Resource created successfully";

    /**
     * 请求处理成功
     */
    public static final String STATUS_200 = "200";
    public static final String MESSAGE_200 = "Request processed successfully";

    /**
     * 期望未满足（通常用于更新/删除失败）
     */
    public static final String STATUS_417 = "417";
    public static final String MESSAGE_417_UPDATE = "Update operation failed. Please try again or contact Dev team";
    public static final String MESSAGE_417_DELETE = "Delete operation failed. Please try again or contact Dev team";

    /**
     * 服务器内部错误（备用）
     */
    // public static final String STATUS_500 = "500";
    // public static final String MESSAGE_500 = "An error occurred. Please try again or contact Dev team";
}
