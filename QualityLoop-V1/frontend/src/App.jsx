import { useEffect, useState } from "react";
import { api } from "./api.js";
import DefectList from "./components/DefectList.jsx";
import DefectForm from "./components/DefectForm.jsx";
import DefectDetail from "./components/DefectDetail.jsx";

export default function App() {
  const [defects, setDefects] = useState([]);
  const [filter, setFilter] = useState("");
  const [selectedId, setSelectedId] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState("");

  const load = async () => {
    try {
      setDefects(await api.list(filter));
      setError("");
    } catch (e) {
      setError(e.message === "Failed to fetch" ? "Cannot reach the backend on port 8080. Is Spring Boot running?" : e.message);
    }
  };

  useEffect(() => { load(); }, [filter]);

  return (
    <>
      <header className="app-header">
        <div className="container d-flex justify-content-between align-items-center">
          <div>
            <h1>QualityLoop</h1>
            <small>Quality and corrective action management</small>
          </div>
          <button className="btn btn-success" onClick={() => { setShowForm(true); setSelectedId(null); }}>
            Report defect
          </button>
        </div>
      </header>

      <main className="container my-4">
        {error && <div className="alert alert-danger">{error}</div>}
        <div className="row g-4">
          <div className="col-lg-5">
            <DefectList
              defects={defects}
              filter={filter}
              onFilter={setFilter}
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
              <DefectDetail key={selectedId} id={selectedId} onChanged={load} />
            )}
            {!showForm && !selectedId && (
              <div className="panel text-secondary">
                Select a defect to follow it from containment to closure, or report a new one.
              </div>
            )}
          </div>
        </div>
      </main>
    </>
  );
}
