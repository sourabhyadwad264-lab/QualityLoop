import { useEffect, useState } from "react";
import { api, session } from "./api.js";
import Login from "./components/Login.jsx";
import DefectList from "./components/DefectList.jsx";
import DefectForm from "./components/DefectForm.jsx";
import DefectDetail from "./components/DefectDetail.jsx";
import Dashboard from "./components/Dashboard.jsx";
import Users from "./components/Users.jsx";
import SecurityLog from "./components/SecurityLog.jsx";
import Account from "./components/Account.jsx";
import PublicReport from "./components/PublicReport.jsx";
import ThemeToggle from "./components/ThemeToggle.jsx";

const ROLE_LABEL = { ADMIN: "Admin", QUALITY_HEAD: "Quality Head", QUALITY_ENGINEER: "Quality Engineer" };

function useTheme() {
  const [theme, setTheme] = useState(
    () => localStorage.getItem("ql_theme") || (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light")
  );
  useEffect(() => {
    document.documentElement.setAttribute("data-bs-theme", theme);
    localStorage.setItem("ql_theme", theme);
  }, [theme]);
  return [theme, () => setTheme(theme === "dark" ? "light" : "dark")];
}

export default function App() {
  const [hash, setHash] = useState(window.location.hash);
  const [user, setUser] = useState(session.user());
  const [notice, setNotice] = useState("");
  const [theme, toggleTheme] = useTheme();

  useEffect(() => {
    const onHash = () => setHash(window.location.hash);
    window.addEventListener("hashchange", onHash);
    return () => window.removeEventListener("hashchange", onHash);
  }, []);

  // A report link opens without login
  if (hash.startsWith("#/report/")) return <PublicReport token={hash.replace("#/report/", "")} />;

  if (!user) {
    return (
      <>
        <ThemeToggle theme={theme} onToggle={toggleTheme} floating />
        <Login notice={notice} onLogin={(auth) => { session.save(auth); setNotice(""); setUser(session.user()); }} />
      </>
    );
  }

  const logout = (message = "") => { session.clear(); setNotice(message); setUser(null); };
  return <Main user={user} theme={theme} onToggleTheme={toggleTheme} onLogout={() => logout()}
               onPasswordChanged={() => logout("Password changed. Log in with your new password.")} />;
}

const Icon = ({ d }) => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d={d} /></svg>
);
const ICONS = {
  dashboard: "M3 3h7v9H3zM14 3h7v5h-7zM14 12h7v9h-7zM3 16h7v5H3z",
  defects: "M12 9v4M12 17h.01M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z",
  users: "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM23 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8",
  security: "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z",
  account: "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z",
  logout: "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9",
};

function Main({ user, theme, onToggleTheme, onLogout, onPasswordChanged }) {
  const [tab, setTab] = useState("dashboard");
  const [defects, setDefects] = useState([]);
  const [filter, setFilter] = useState("");
  const [search, setSearch] = useState("");
  const [selectedId, setSelectedId] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState("");

  const load = async () => {
    try {
      setDefects(await api.list(filter, search));
      setError("");
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => { load(); }, [filter, search]);

  const canManage = user.role === "ADMIN" || user.role === "QUALITY_HEAD";
  const isAdmin = user.role === "ADMIN";

  const items = [
    ["dashboard", "Dashboard", ICONS.dashboard],
    ["defects", "Defects", ICONS.defects],
    ...(isAdmin ? [["users", "Users", ICONS.users], ["security", "Security log", ICONS.security]] : []),
    ["account", "My account", ICONS.account],
  ];
  const titles = { dashboard: "Dashboard", defects: "Defects", users: "Users and roles", security: "Security log", account: "My account" };

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <svg width="28" height="28" viewBox="0 0 32 32" fill="none" aria-hidden="true">
            <circle cx="16" cy="16" r="11" stroke="currentColor" strokeWidth="3" strokeDasharray="58 12" />
            <path d="M27.5 7.5l-.8 7.4-6.2-4z" fill="currentColor" />
          </svg>
          <strong>QualityLoop</strong>
        </div>
        {items.map(([key, label, icon]) => (
          <button key={key} className={"nav-item" + (tab === key ? " on" : "")} onClick={() => setTab(key)}>
            <Icon d={icon} />{label}
          </button>
        ))}
        <div className="nav-spacer" />
        <div className="who">
          <div>{user.fullName}</div>
          <small>{ROLE_LABEL[user.role]}</small>
        </div>
        <button className="nav-item" onClick={onLogout}><Icon d={ICONS.logout} />Log out</button>
      </aside>

      <div className="content">
        <div className="topbar">
          <h1>{titles[tab]}</h1>
          <ThemeToggle theme={theme} onToggle={onToggleTheme} />
        </div>
        <main className="page">
          {error && <div className="alert alert-danger">{error}</div>}

          {tab === "dashboard" && <Dashboard />}
          {tab === "users" && isAdmin && <Users me={user} />}
          {tab === "security" && isAdmin && <SecurityLog />}
          {tab === "account" && <Account user={user} onPasswordChanged={onPasswordChanged} />}

          {tab === "defects" && (
            <div className="row g-4">
              <div className="col-xl-5">
                <div className="d-flex justify-content-end mb-2">
                  <button className="btn btn-success" onClick={() => { setShowForm(true); setSelectedId(null); }}>
                    Report defect
                  </button>
                </div>
                <DefectList
                  defects={defects}
                  filter={filter}
                  onFilter={setFilter}
                  search={search}
                  onSearch={setSearch}
                  selectedId={selectedId}
                  onSelect={(id) => { setSelectedId(id); setShowForm(false); }}
                />
              </div>
              <div className="col-xl-7">
                {showForm && (
                  <DefectForm
                    onSaved={(d) => { setShowForm(false); setSelectedId(d.id); load(); }}
                    onCancel={() => setShowForm(false)}
                  />
                )}
                {!showForm && selectedId && (
                  <DefectDetail
                    key={selectedId}
                    id={selectedId}
                    user={user}
                    canManage={canManage}
                    onChanged={load}
                    onDeleted={() => { setSelectedId(null); load(); }}
                  />
                )}
                {!showForm && !selectedId && (
                  <div className="panel text-secondary">
                    Select a defect to follow it from containment to closure, or report a new one.
                  </div>
                )}
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
