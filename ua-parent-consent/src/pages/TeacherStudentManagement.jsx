
import { useCallback, useEffect, useState } from "react";
import TeacherSidebar from "../components/TeacherSidebar";
import { apiRequest } from "../services/api";

function TeacherStudentManagement() {
  const [sections, setSections] = useState([]);
  const [students, setStudents] = useState([]);
  const [sectionId, setSectionId] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const [studentNumber, setStudentNumber] = useState("");
  const [fullName, setFullName] = useState("");
  const [editingId, setEditingId] = useState(null);

  const fetchSections = useCallback(async () => {
    try {
      setError("");

      const data = await apiRequest("/teacher/sections/my-sections");
      const sectionList = Array.isArray(data) ? data : [];

      setSections(sectionList);

      if (sectionList.length > 0) {
        setSectionId((current) =>
          current || String(sectionList[0].id)
        );
      }
    } catch (err) {
      setError(err.message || "Failed to load assigned sections.");
    }
  }, []);

  const fetchStudents = useCallback(async () => {
    if (!sectionId) {
      setStudents([]);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError("");

      const data = await apiRequest(
        `/teacher/students/section/${sectionId}`
      );

      setStudents(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Failed to load students.");
    } finally {
      setLoading(false);
    }
  }, [sectionId]);

  useEffect(() => {
    fetchSections();
  }, [fetchSections]);

  useEffect(() => {
    fetchStudents();
  }, [fetchStudents]);

  const resetForm = () => {
    setStudentNumber("");
    setFullName("");
    setEditingId(null);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!studentNumber.trim() || !fullName.trim() || !sectionId) {
      setError("Please complete all student information.");
      return;
    }

    try {
      setSaving(true);
      setError("");
      setMessage("");

      const payload = {
        studentNumber: studentNumber.trim(),
        fullName: fullName.trim(),
        sectionId: Number(sectionId),
      };

      if (editingId) {
        await apiRequest(`/teacher/students/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });

        setMessage("Student information updated successfully.");
      } else {
        await apiRequest("/teacher/students", {
          method: "POST",
          body: JSON.stringify(payload),
        });

        setMessage("Student added successfully.");
      }

      resetForm();
      await fetchStudents();
    } catch (err) {
      setError(err.message || "Unable to save student.");
    } finally {
      setSaving(false);
    }
  };

  const handleEdit = (student) => {
    setEditingId(student.id);
    setStudentNumber(student.studentNumber);
    setFullName(student.fullName);
    setSectionId(String(student.sectionId));
    setMessage("");
    setError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleDeactivate = async (student) => {
    const confirmed = window.confirm(
      `Are you sure you want to deactivate ${student.fullName}?`
    );

    if (!confirmed) return;

    try {
      setError("");
      setMessage("");

      await apiRequest(
        `/teacher/students/${student.id}/deactivate`,
        { method: "PATCH" }
      );

      setMessage("Student deactivated successfully.");
      await fetchStudents();
    } catch (err) {
      setError(err.message || "Unable to deactivate student.");
    }
  };

  return (
    <div className="parent-layout">
      <TeacherSidebar />

      <main className="parent-main teacher-dashboard-redesign">
        <header className="dashboard-header teacher-new-header">
          <div>
            <span className="teacher-page-label">
              FACULTY PORTAL
            </span>
            <h1>Student Management</h1>
            <p>
              Add and manage students in your assigned sections.
            </p>
          </div>
        </header>

        {message && (
          <div className="teacher-success-message">
            {message}
          </div>
        )}

        {error && (
          <div className="teacher-error-message">
            {error}
          </div>
        )}

        <section className="content-card teacher-student-form-card">
          <div className="section-heading">
            <div>
              <h2>{editingId ? "Edit Student" : "Add New Student"}</h2>
              <p>Enter the student's information below.</p>
            </div>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="teacher-student-form-grid">
              <div>
                <label htmlFor="studentNumber">
                  Student Number
                </label>
                <input
                  id="studentNumber"
                  type="text"
                  value={studentNumber}
                  onChange={(e) => setStudentNumber(e.target.value)}
                  placeholder="Enter student number"
                  required
                />
              </div>

              <div>
                <label htmlFor="fullName">
                  Full Name
                </label>
                <input
                  id="fullName"
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Enter student's full name"
                  required
                />
              </div>

              <div>
                <label htmlFor="sectionId">
                  Section
                </label>
                <select
                  id="sectionId"
                  value={sectionId}
                  onChange={(e) => setSectionId(e.target.value)}
                  required
                >
                  <option value="">Select section</option>
                  {sections.map((section) => (
                    <option key={section.id} value={section.id}>
                      {section.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="teacher-student-form-actions">
              <button
                type="submit"
                disabled={saving || sections.length === 0}
              >
                {saving
                  ? "Saving..."
                  : editingId
                    ? "Update Student"
                    : "Add Student"}
              </button>

              {editingId && (
                <button
                  type="button"
                  onClick={resetForm}
                  disabled={saving}
                >
                  Cancel
                </button>
              )}
            </div>
          </form>

          {sections.length === 0 && (
            <p>
              No assigned sections found. Please contact your
              administrator to assign a section.
            </p>
          )}
        </section>

        <section className="content-card teacher-new-card">
          <div className="section-heading">
            <div>
              <h2>Student Roster</h2>
              <p>Students registered under your assigned sections.</p>
            </div>

            <span className="total-label">
              {students.length} Students
            </span>
          </div>

          <div className="teacher-student-filter">
            <label htmlFor="studentSectionFilter">
              Filter by Section
            </label>

            <select
              id="studentSectionFilter"
              value={sectionId}
              onChange={(e) => setSectionId(e.target.value)}
            >
              {sections.map((section) => (
                <option key={section.id} value={section.id}>
                  {section.name}
                </option>
              ))}
            </select>
          </div>

          {loading ? (
            <p>Loading students...</p>
          ) : students.length === 0 ? (
            <div className="teacher-empty-state">
              <h3>No Students Found</h3>
              <p>
                Add a student to this section to get started.
              </p>
            </div>
          ) : (
            <div className="table-container">
              <table className="teacher-student-table">
                <thead>
                  <tr>
                    <th>Student Number</th>
                    <th>Full Name</th>
                    <th>Section</th>
                    <th>Actions</th>
                  </tr>
                </thead>

                <tbody>
                  {students.map((student) => (
                    <tr key={student.id}>
                      <td>{student.studentNumber}</td>
                      <td>{student.fullName}</td>
                      <td>{student.sectionName}</td>
                      <td>
                        <div className="teacher-student-actions">
                          <button
                            type="button"
                            onClick={() => handleEdit(student)}
                          >
                            Edit
                          </button>

                          <button
                            type="button"
                            onClick={() => handleDeactivate(student)}
                          >
                            Deactivate
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

export default TeacherStudentManagement;
