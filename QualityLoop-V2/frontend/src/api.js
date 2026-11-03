// All backend calls live here. Uses the browser's built-in fetch.
const ROOT = "http://localhost:8081/api";

// ---- login session (kept in the browser so a refresh does not log you out) ----
export const session = {
  token: () => localStorage.getItem("ql_token"),
  user: () => {
    const raw = localStorage.getItem("ql_user");
    return raw ? JSON.parse(raw) : null;
  },
  save: (auth) => {
    localStorage.setItem("ql_token", auth.token);
    localStorage.setItem("ql_user", JSON.stringify({ username: auth.username, fullName: auth.fullName, role: auth.role }));
  },
  clear: () => {
    localStorage.removeItem("ql_token");
    localStorage.removeItem("ql_user");
  },
};

async function request(path = "", options = {}) {
  const headers = { "Content-Type": "application/json" };
  const token = session.token();
  if (token) headers.Authorization = "Bearer " + token;

  let res;
  try {
    res = await fetch(ROOT + path, { ...options, headers });
  } catch {
    throw new Error("Cannot reach the backend on port 8080. Is Spring Boot running?");
  }

  // Logged-in user whose token expired or was rejected: go back to the login page
  if (res.status === 401 && token) {
    session.clear();
    window.location.hash = "";
    window.location.reload();
  }

  const text = await res.text();
  const body = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error(body?.error || "Request failed (" + res.status + ")");
  return body;
}

const post = (data) => ({ method: "POST", body: JSON.stringify(data) });
const patch = (data) => ({ method: "PATCH", body: data === undefined ? undefined : JSON.stringify(data) });

export const api = {
  // account
  login: (data) => request("/auth/login", post(data)),
  register: (data) => request("/auth/register", post(data)),

  // defects
  list: (status, q) => {
    const p = new URLSearchParams();
    if (status) p.set("status", status);
    if (q) p.set("q", q);
    const qs = p.toString();
    return request("/defects" + (qs ? "?" + qs : ""));
  },
  get: (id) => request("/defects/" + id),
  create: (data) => request("/defects", post(data)),
  update: (id, data) => request("/defects/" + id, { method: "PUT", body: JSON.stringify(data) }),
  remove: (id) => request("/defects/" + id, { method: "DELETE" }),
  setStatus: (id, status) => request("/defects/" + id + "/status", patch({ status })),
  addAction: (id, data) => request("/defects/" + id + "/actions", post(data)),
  completeAction: (id, actionId) => request("/defects/" + id + "/actions/" + actionId + "/complete", patch()),
  removeAction: (id, actionId) => request("/defects/" + id + "/actions/" + actionId, { method: "DELETE" }),
  audit: (id) => request("/defects/" + id + "/audit"),

  // reports and sharing
  summary: () => request("/reports/summary"),
  share: (id) => request("/defects/" + id + "/share", post({})),
  emailReport: (id, data) => request("/defects/" + id + "/email", post(data)),
  publicReport: (token) => request("/public/reports/" + token),

  // admin
  users: () => request("/users"),
  updateUser: (id, data) => request("/users/" + id, patch(data)),
};
