/* eslint-disable */
import request from "@/request"

/** GET /health/ */
export async function healthCheck(options?: { [key: string]: any }) {
  return request<API.BaseResponseString>("/health/", {
    method: "GET",
    ...(options || {}),
  })
}
