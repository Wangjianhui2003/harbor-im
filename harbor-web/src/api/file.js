import http from "./http.js";

/**
 * @param formData 表单数据
 * @param url0 需要访问的url
 * @param headers0 请求头
 */
const uploadFile = (formData, url_, headers_) => {
  return http({
    url: url_,
    method: "POST",
    data: formData,
    headers: headers_,
  });
};

export default uploadFile;
