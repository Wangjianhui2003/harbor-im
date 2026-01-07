import http from "./http.ts";

export const refreshToken = async (refreshToken: string): Promise<any> => {
  try {
    return await http({
      method: "put",
      url: "/refreshToken",
      headers: {
        refreshToken: refreshToken,
      },
    });
  } catch {
    location.href = "/";
  }
};
