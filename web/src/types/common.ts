/**
 * 统一响应体与分页结构（与后端 CommonResult / CommonPage 对应）。
 */

/** 非流式接口的统一返回：code === 200 表示成功。 */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页数据（位于 ApiResult.data 内）。 */
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  totalPage: number
  total: number
  list: T[]
}

/** 分页查询参数。 */
export interface PageQuery {
  pageNum?: number
  pageSize?: number
}

/** 后端错误码，见 spec §5。 */
export type ErrorCode =
  | 40001 // 参数错误
  | 40101 // 未登录 / 登录过期
  | 40301 // 越权
  | 40401 // 不存在
  | 42901 // 限流
  | 42902 // 配额耗尽
  | 50001 // 上游失败
