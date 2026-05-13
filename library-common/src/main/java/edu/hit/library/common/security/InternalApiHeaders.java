package edu.hit.library.common.security;

/** 服务间受保护接口使用的 HTTP 头名称（与图书库存等内部能力对齐）。 */
public final class InternalApiHeaders {

    public static final String SERVICE_TOKEN = "X-Internal-Token";

    private InternalApiHeaders() {}
}
