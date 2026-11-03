import { useEffect, useState } from "react";
import { api, session } from "./api.js";
import Login from "./components/Login.jsx";
import DefectList from "./components/DefectList.jsx";
import DefectForm from "./components/DefectForm.jsx";
import DefectDetail from "./components/DefectDetail.jsx";
import Dashboard from "./components/Dashboard.jsx";
import Users from "./components/Users.jsx";
import PublicReport from "./components/PublicReport.jsx";

const ROLE_LABEL = { ADMIN: "Admin", QUALITY_HEAD: "Quality Head", QUALITY_ENGINEER: "Quality Engineer" };

export default function App() {
  const [hash, setHash] = useState(window.location.hash);
  const [user, setUser] = useState(session.user());

  useEffect(() => {
    const onHash = () => setHash(window.location.hash);
    window.addEventListener("hashchange", onHash);
    return () => window.removeEventListener("hashchange", onHash);
  }, []);

  // A report link opens without login
  if (hash.startsWith("#/report/")) return <PublicReport token={hash.replace("#/report/", "")} />;

  if (!user) return <Login onLogin={(auth) => { session.save(auth); setUser(session.user()); }} />;

  return <Main user={user} onLogout={() => { session.clear(); setUser(null); }} />;
}

function Main({ user, onLogout }) {
  const [tab, setTab] = useState("defects");
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

  return (
    <>
      <header className="app-header">
        <div className="container">
          <div className="d-flex justify-content-between align-items-center">
            <div>
              <h1>QualityLoop</h1>
              <small>Quality and corrective action management</small>
            </div>
            <div className="text-end">
              <div>{user.fullName}</div>
              <small>{ROLE_LABEL[user.role]}</small>
              <button className="btn btn-sm btn-outline-light ms-3" onClick={onLogout}>Log out</button>
            </div>
          </div>
          <div className="tabs">
            <button className={tab === "defects" ? "on" : ""} onClick={() => setTab("defects")}>Defects</button>
            <button className={tab === "dashboard" ? "on" : ""} onClick={() => setTab("dashboard")}>Dashboard</button>
            {user.role === "ADMIN" && (
              <button className={tab === "users" ? "on" : ""} onClick={() => setTab("users")}>Users</button>
            )}
          </div>
        </div>
      </header>

      <main className="container my-4">
        {error && <div className="alert alert-danger">{error}</div>}

        {tab === "dashboard" && <Dashboard />}
        {tab === "users" && user.role === "ADMIN" && <Users me={user} />}

        {tab === "defects" && (
          <div className="row g-4">
            <div className="col-lg-5">
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
            <div className="col-lg-7">
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
    </>
  );
}
