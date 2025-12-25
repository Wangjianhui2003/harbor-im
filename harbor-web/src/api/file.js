import http from "./http.js";

/**
 * @param formData 表单数据
 * @param url0 需要访问的url
 * @param headers0 请求头
 */
const uploadFile = (formData, url0, headers0) => {
  return http({
    url: url0,
    method: "POST",
    data: formData,
    headers: headers0,
  });
};

export default uploadFile;
