// All backend calls live here. Uses the browser's built-in fetch.
const BASE = "http://localhost:8080/api/defects";

async function request(path = "", options = {}) {
  const res = await fetch(BASE + path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  const body = res.status === 204 ? null : await res.json();
  if (!res.ok) throw new Error(body?.error || "Request failed (" + res.status + ")");
  return body;
}

export const api = {
  list: (status) => request(status ? "?status=" + status : ""),
  get: (id) => request("/" + id),
  create: (data) => request("", { method: "POST", body: JSON.stringify(data) }),
  update: (id, data) => request("/" + id, { method: "PUT", body: JSON.stringify(data) }),
  setStatus: (id, status) =>
    request("/" + id + "/status", { method: "PATCH", body: JSON.stringify({ status }) }),
  addAction: (id, data) =>
    request("/" + id + "/actions", { method: "POST", body: JSON.stringify(data) }),
  completeAction: (id, actionId) =>
    request("/" + id + "/actions/" + actionId + "/complete", { method: "PATCH" }),
  audit: (id) => request("/" + id + "/audit"),
};
