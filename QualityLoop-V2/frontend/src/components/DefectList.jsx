export default function DefectList({ defects, filter, onFilter, search, onSearch, selectedId, onSelect }) {
  return (
    <div className="panel">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2 className="h5 m-0">Defects</h2>
        <select className="form-select form-select-sm w-auto" value={filter} onChange={(e) => onFilter(e.target.value)}>
          <option value="">All</option>
          <option value="OPEN">Open</option>
          <option value="IN_PROGRESS">In progress</option>
          <option value="CLOSED">Closed</option>
        </select>
      </div>
      <input
        className="form-control form-control-sm mb-3"
        placeholder="Search by title, part number or part name"
        value={search}
        onChange={(e) => onSearch(e.target.value)}
      />
      {defects.length === 0 && <p className="text-secondary m-0">No defects found. Report one to start the loop.</p>}
      <div className="list-group">
        {defects.map((d) => (
          <button
            key={d.id}
            className={"list-group-item list-group-item-action defect-row sev-" + d.severity}
            onClick={() => onSelect(d.id)}
          >
            <div className="d-flex justify-content-between">
              <strong>#{d.id} {d.title}</strong>
              <span className="badge text-bg-secondary">{d.status.replace("_", " ")}</span>
            </div>
            <small className="text-secondary">
              Part {d.partNumber || "-"}, {d.severity} severity
            </small>
          </button>
        ))}
      </div>
    </div>
  );
}
