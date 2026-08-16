import { http } from "@/utils/http";
import type { MaterialAsset, PageParam, PageResult } from "./types";

export const getMaterialPage = (
  params: PageParam & {
    bizType?: string;
    keyword?: string;
    createdBy?: number;
    startTime?: string;
    endTime?: string;
  }
) => {
  return http.get<PageResult<MaterialAsset>, typeof params>("/admin-api/material/page", { params });
};

export const uploadMaterial = (file: File, bizType: string) => {
  const formData = new FormData();
  formData.append("file", file);
  if (bizType) {
    formData.append("bizType", bizType);
  }
  return http.post<MaterialAsset, FormData>("/admin-api/material/upload", {
    data: formData,
    headers: { "Content-Type": "multipart/form-data" }
  });
};

export const getMaterialReferences = (id: number) => {
  return http.get<string[], { id: number }>("/admin-api/material/references", { params: { id } });
};

export const deleteMaterial = (id: number) => {
  return http.request<boolean>("delete", "/admin-api/material/delete", { params: { id } });
};
