/* eslint-disable */
import request from "@/request"

/** GET /static/${param0}/** */
export async function serveStaticResource(
  // Parameter type added by generation for non-body params
  params: API.serveStaticResourceParams,
  options?: { [key: string]: any },
) {
  const { deployKey: param0, ...queryParams } = params
  return request<string>(`/static/${param0}/**`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  })
}
