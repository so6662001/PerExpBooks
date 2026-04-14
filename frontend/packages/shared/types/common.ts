export interface Result<T = any> {
  code: number
  message: string
  data: T
}

export interface PageResult<T = any> {
  total: number
  list: T[]
  pageNum: number
  pageSize: number
}

export interface PageQuery {
  pageNum?: number
  pageSize?: number
}

export type StatusType = 'pending' | 'approved' | 'rejected' | 'completed'
