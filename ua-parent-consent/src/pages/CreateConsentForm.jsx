
import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import TeacherSidebar from "../components/TeacherSidebar";
import { apiRequest } from "../services/api";

function CreateConsentForm() {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEditMode = Boolean(id);

  const [formData, setFormData] = useState({
    title: "",
    description: "",
    className: "",
    course: "",
    professor: "",
    activityDate: "",
    location: "",
    deadline: "",
    instructions: ""
  });

  const [sections, setSections] = useState([]);
  const [sectionId, setSectionId] = useState("");
  const [students, setStudents] = useState([]);
  const [selectedStudentIds, setSelectedStudentIds] = useState([]);

  const [sectionsLoading, setSectionsLoading] = useState(true);
  const [studentsLoading, setStudentsLoading] = useState(false);
  const [formLoading, setFormLoading] = useState(isEditMode);

  const [studentError, setStudentError] = useState("");
  const [formError, setFormError] = useState("");

  const [showPreview, setShowPreview] = useState(false);
  const [publishing, setPublishing] = useState(false);

  const today = new Date().toLocaleDateString("en-CA");

  // Load teacher's sections
  const fetchSections = useCallback(async () => {
    try {
      setSectionsLoading(true);
      setStudentError("");

      const data = await apiRequest("/teacher/sections/my-sections");
      const sectionList = Array.isArray(data) ? data : [];

      setSections(sectionList);

      if (!isEditMode && sectionList.length > 0) {
        setSectionId(String(sectionList[0].id));
      }
    } catch (error) {
      setStudentError(error.message || "Unable to load sections.");
    } finally {
      setSectionsLoading(false);
    }
  }, [isEditMode]);

  // Load existing draft when editing
  const fetchDraft = useCallback(async () => {
    if (!isEditMode) return;

    try {
      setFormLoading(true);
      setFormError("");

      const data = await apiRequest(
        `/teacher/consent-forms/${id}`
      );

      if (data.status?.toLowerCase() !== "draft") {
        setFormError(
          "Only draft consent forms can be edited."
        );
        return;
      }

      setFormData({
        title: data.title || "",
        description: data.description || "",
        className: data.className || "",
        course: data.course || "",
        professor: data.professor || "",
        activityDate: data.activityDate || "",
        location: data.location || "",
        deadline: data.deadline || "",
        instructions: data.instructions || ""
      });

      setSelectedStudentIds(
        (data.studentIds || []).map(Number)
      );
    } catch (error) {
      setFormError(
        error.message || "Unable to load draft consent form."
      );
    } finally {
      setFormLoading(false);
    }
  }, [id, isEditMode]);

  useEffect(() => {
    fetchSections();
  }, [fetchSections]);

  useEffect(() => {
    fetchDraft();
  }, [fetchDraft]);

  // Load students from selected section
  const fetchStudents = useCallback(async () => {
    if (!sectionId) {
      setStudents([]);
      return;
    }

    try {
      setStudentsLoading(true);
      setStudentError("");

      const data = await apiRequest(
        `/teacher/students/section/${sectionId}`
      );

      const activeStudents = Array.isArray(data)
        ? data.filter((student) => student.active !== false)
        : [];

      setStudents(activeStudents);
    } catch (error) {
      setStudents([]);
      setStudentError(error.message || "Unable to load students.");
    } finally {
      setStudentsLoading(false);
    }
  }, [sectionId]);

  useEffect(() => {
    if (!sectionsLoading && !formLoading && sectionId) {
      fetchStudents();
    }
  }, [
    fetchStudents,
    sectionsLoading,
    formLoading,
    sectionId
  ]);

  // Find the section containing the existing assigned students
  useEffect(() => {
    if (
      !isEditMode ||
      sectionsLoading ||
      formLoading ||
      sections.length === 0 ||
      selectedStudentIds.length === 0
    ) {
      return;
    }

    let cancelled = false;

    const findAssignedSection = async () => {
      for (const section of sections) {
        try {
          const data = await apiRequest(
            `/teacher/students/section/${section.id}`
          );

          const sectionStudents = Array.isArray(data)
            ? data.filter((student) => student.active !== false)
            : [];

          const hasAssignedStudent = sectionStudents.some(
            (student) => selectedStudentIds.includes(Number(student.id))
          );

          if (hasAssignedStudent) {
            if (!cancelled) {
              setSectionId(String(section.id));
            }
            return;
          }
        } catch (error) {
          console.error(
            `Unable to check section ${section.id}:`,
            error
          );
        }
      }
    };

    findAssignedSection();

    return () => {
      cancelled = true;
    };
  }, [
    isEditMode,
    sections,
    sectionsLoading,
    formLoading,
    selectedStudentIds
  ]);

  const handleChange = (e) => {
    const { name, value } = e.target;

    setFormData((previous) => ({
      ...previous,
      [name]: value
    }));
  };

  const handleSectionChange = (e) => {
    setSectionId(e.target.value);
    setSelectedStudentIds([]);
  };

  const handleStudentToggle = (studentId) => {
    setSelectedStudentIds((previous) =>
      previous.includes(studentId)
        ? previous.filter((id) => id !== studentId)
        : [...previous, studentId]
    );
  };

  const handleSelectAll = () => {
    const allIds = students.map((student) => student.id);

    const allSelected =
      allIds.length > 0 &&
      allIds.every((id) => selectedStudentIds.includes(id));

    if (allSelected) {
      setSelectedStudentIds((previous) =>
        previous.filter((id) => !allIds.includes(id))
      );
    } else {
      setSelectedStudentIds((previous) =>
        [...new Set([...previous, ...allIds])]
      );
    }
  };

  const handlePreview = (e) => {
    e.preventDefault();

    if (formData.deadline > formData.activityDate) {
      alert("The submission deadline cannot be after the activity date.");
      return;
    }

    if (selectedStudentIds.length === 0) {
      alert("Please select at least one student.");
      return;
    }

    setShowPreview(true);
  };

  // Save new form or update existing draft
  const handleSave = async (status) => {
    if (publishing) return;

    if (selectedStudentIds.length === 0) {
      alert("Please select at least one student.");
      return;
    }

    if (formData.deadline > formData.activityDate) {
      alert("The submission deadline cannot be after the activity date.");
      return;
    }

    setPublishing(true);

    try {
      const payload = {
        ...formData,
        studentIds: selectedStudentIds.map(Number)
      };

      let savedForm;

      if (isEditMode) {
        if (status !== "Draft") {
          alert("Please publish the draft from its details page.");
          return;
        }

        savedForm = await apiRequest(
          `/teacher/consent-forms/${id}`,
          {
            method: "PUT",
            body: JSON.stringify(payload)
          }
        );

        alert("Draft updated successfully!");
      } else {
        savedForm = await apiRequest(
          `/teacher/consent-forms?status=${status}`,
          {
            method: "POST",
            body: JSON.stringify(payload)
          }
        );

        alert(
          status === "Draft"
            ? "Consent form saved as draft successfully!"
            : "Consent form published successfully!"
        );
      }

      navigate("/teacher-consent-forms", {
        state: { newForm: savedForm }
      });
    } catch (error) {
      alert(
        error.message ||
        (isEditMode
          ? "Unable to update draft."
          : "Unable to save consent form.")
      );
    } finally {
      setPublishing(false);
    }
  };

  const allStudentsSelected =
    students.length > 0 &&
    students.every((student) =>
      selectedStudentIds.includes(student.id)
    );

  const selectedStudents = students.filter((student) =>
    selectedStudentIds.includes(student.id)
  );

  if (formLoading || sectionsLoading) {
    return (
      <div className="parent-layout">
        <TeacherSidebar />
        <main className="parent-main create-consent-page">
          <p>Loading consent form...</p>
        </main>
      </div>
    );
  }

  if (formError) {
    return (
      <div className="parent-layout">
        <TeacherSidebar />
        <main className="parent-main create-consent-page">
          <section className="create-consent-card">
            <p className="create-consent-student-error">
              {formError}
            </p>

            <button
              type="button"
              className="create-consent-secondary"
              onClick={() => navigate("/teacher-consent-forms")}
            >
              Back to Consent Forms
            </button>
          </section>
        </main>
      </div>
    );
  }

  return (
    <div className="parent-layout">
      <TeacherSidebar />

      <main className="parent-main create-consent-page">
        <header className="dashboard-header create-consent-header">
          <div>
            <span className="create-consent-eyebrow">
              TEACHER WORKSPACE
            </span>

            <h1>
              {isEditMode
                ? "Edit Draft Consent Form"
                : "Create Digital Consent Form"}
            </h1>

            <p>
              {isEditMode
                ? "Update the information and assigned students in your draft."
                : "Prepare and publish an activity consent form for parents and guardians."}
            </p>
          </div>

          <button
            type="button"
            className="create-consent-cancel"
            onClick={() =>
              navigate(
                isEditMode
                  ? `/teacher-consent-form/${id}`
                  : "/teacher-dashboard"
              )
            }
          >
            {isEditMode ? "Back to Details" : "Back to Dashboard"}
          </button>
        </header>

        {!showPreview ? (
          <section className="create-consent-card">
            <div className="create-consent-card-heading">
              <div className="create-consent-heading-icon">✎</div>

              <div>
                <h2>Consent Form Information</h2>
                <p>
                  Fill in the activity details before saving your form.
                </p>
              </div>
            </div>

            <div className="create-consent-progress">
              <span className="create-consent-step active">1</span>
              <span className="create-consent-progress-line"></span>
              <span className="create-consent-step">2</span>
              <span className="create-consent-progress-label">
                Form Details <span>→</span> Preview
              </span>
            </div>

            <form onSubmit={handlePreview} className="create-consent-form">
              <div className="create-consent-section-title">
                <h3>Basic Information</h3>
                <p>Provide the main details of the activity.</p>
              </div>

              <div className="create-consent-field">
                <label htmlFor="title">
                  Form Title <span>*</span>
                </label>
                <input
                  id="title"
                  type="text"
                  name="title"
                  placeholder="e.g. Educational Field Trip"
                  value={formData.title}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="create-consent-field">
                <label htmlFor="description">
                  Activity Description <span>*</span>
                </label>
                <textarea
                  id="description"
                  name="description"
                  placeholder="Explain the purpose and details of the activity..."
                  value={formData.description}
                  onChange={handleChange}
                  rows="4"
                  required
                />
              </div>

              <div className="create-consent-field">
                <label htmlFor="className">
                  Class / Section <span>*</span>
                </label>
                <input
                  id="className"
                  type="text"
                  name="className"
                  placeholder="e.g. BSIT DA3A"
                  value={formData.className}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="create-consent-grid">
                <div className="create-consent-field">
                  <label htmlFor="course">
                    Course <span>*</span>
                  </label>
                  <input
                    id="course"
                    type="text"
                    name="course"
                    placeholder="e.g. Data Analytics Fundamentals"
                    value={formData.course}
                    onChange={handleChange}
                    required
                  />
                </div>

                <div className="create-consent-field">
                  <label htmlFor="professor">
                    Professor <span>*</span>
                  </label>
                  <input
                    id="professor"
                    type="text"
                    name="professor"
                    placeholder="Enter professor's name"
                    value={formData.professor}
                    onChange={handleChange}
                    required
                  />
                </div>
              </div>

              <div className="create-consent-section-title create-consent-second-section">
                <h3>Activity Schedule and Location</h3>
                <p>Set the activity date and parent submission deadline.</p>
              </div>

              <div className="create-consent-grid">
                <div className="create-consent-field">
                  <label htmlFor="activityDate">
                    Activity Date <span>*</span>
                  </label>
                  <input
                    id="activityDate"
                    type="date"
                    name="activityDate"
                    value={formData.activityDate}
                    onChange={handleChange}
                    min={today}
                    required
                  />
                </div>

                <div className="create-consent-field">
                  <label htmlFor="deadline">
                    Submission Deadline <span>*</span>
                  </label>
                  <input
                    id="deadline"
                    type="date"
                    name="deadline"
                    value={formData.deadline}
                    onChange={handleChange}
                    min={today}
                    max={formData.activityDate || undefined}
                    required
                  />
                </div>
              </div>

              <div className="create-consent-field">
                <label htmlFor="location">
                  Activity Location <span>*</span>
                </label>
                <input
                  id="location"
                  type="text"
                  name="location"
                  placeholder="Enter the activity venue or location"
                  value={formData.location}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="create-consent-section-title create-consent-second-section">
                <h3>Additional Information</h3>
                <p>Add reminders or special instructions for parents.</p>
              </div>

              <div className="create-consent-field">
                <label htmlFor="instructions">
                  Additional Instructions
                </label>
                <textarea
                  id="instructions"
                  name="instructions"
                  placeholder="Enter reminders, requirements, or other important information..."
                  value={formData.instructions}
                  onChange={handleChange}
                  rows="4"
                />
              </div>

              <div className="create-consent-section-title create-consent-second-section">
                <h3>Select Students <span>*</span></h3>
                <p>
                  Choose the students who should receive this consent form.
                </p>
              </div>

              <div className="create-consent-field">
                <label htmlFor="studentSection">
                  Select Section
                </label>

                <select
                  id="studentSection"
                  value={sectionId}
                  onChange={handleSectionChange}
                  disabled={sections.length === 0}
                >
                  <option value="">Select a section</option>

                  {sections.map((section) => (
                    <option key={section.id} value={section.id}>
                      {section.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="create-consent-student-selection">
                <div className="create-consent-student-heading">
                  <div>
                    <h4>Student Roster</h4>
                    <p>
                      {selectedStudentIds.length} student(s) selected
                    </p>
                  </div>

                  <button
                    type="button"
                    onClick={handleSelectAll}
                    disabled={studentsLoading || students.length === 0}
                  >
                    {allStudentsSelected ? "Deselect All" : "Select All"}
                  </button>
                </div>

                {studentsLoading ? (
                  <p className="create-consent-student-message">
                    Loading students...
                  </p>
                ) : studentError ? (
                  <p className="create-consent-student-error">
                    {studentError}
                  </p>
                ) : sections.length === 0 ? (
                  <p className="create-consent-student-message">
                    No assigned sections found.
                  </p>
                ) : !sectionId ? (
                  <p className="create-consent-student-message">
                    Please select a section.
                  </p>
                ) : students.length === 0 ? (
                  <p className="create-consent-student-message">
                    No active students found in this section.
                  </p>
                ) : (
                  <div className="create-consent-student-list">
                    {students.map((student) => (
                      <label
                        key={student.id}
                        className="create-consent-student-option"
                      >
                        <input
                          type="checkbox"
                          checked={selectedStudentIds.includes(student.id)}
                          onChange={() => handleStudentToggle(student.id)}
                        />

                        <span>
                          <strong>{student.fullName}</strong>
                          <small>{student.studentNumber}</small>
                        </span>
                      </label>
                    ))}
                  </div>
                )}
              </div>

              <div className="create-consent-note">
                <span>ⓘ</span>
                <p>
                  Please review all activity details carefully. You will be
                  able to preview the consent form before saving.
                </p>
              </div>

              <div className="create-consent-actions">
                <button
                  type="button"
                  className="create-consent-secondary"
                  onClick={() =>
                    navigate(
                      isEditMode
                        ? `/teacher-consent-form/${id}`
                        : "/teacher-dashboard"
                    )
                  }
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="create-consent-primary"
                >
                  Preview Consent Form <span>→</span>
                </button>
              </div>
            </form>
          </section>
        ) : (
          <section className="create-consent-card">
            <div className="create-consent-card-heading">
              <div className="create-consent-heading-icon">▤</div>

              <div>
                <h2>Review Your Consent Form</h2>
                <p>
                  Check the information below before saving your changes.
                </p>
              </div>
            </div>

            <div className="create-consent-progress">
              <span className="create-consent-step completed">✓</span>
              <span className="create-consent-progress-line completed"></span>
              <span className="create-consent-step active">2</span>
              <span className="create-consent-progress-label">
                Form Details <span>→</span> Preview
              </span>
            </div>

            <article className="create-consent-document">
              <div className="create-consent-document-top">
                <div className="create-consent-university-mark">UA</div>

                <div>
                  <h2>UNIVERSITY OF THE ASSUMPTION</h2>
                  <p>Parent / Guardian Digital Consent Form</p>
                </div>
              </div>

              <div className="create-consent-document-divider"></div>

              <div className="create-consent-document-body">
                <span className="create-consent-document-label">
                  ACTIVITY CONSENT FORM
                </span>

                <h2>{formData.title}</h2>

                <div className="create-consent-document-details">
                  <div>
                    <span>CLASS / SECTION</span>
                    <strong>{formData.className}</strong>
                  </div>

                  <div>
                    <span>COURSE</span>
                    <strong>{formData.course}</strong>
                  </div>

                  <div>
                    <span>PROFESSOR</span>
                    <strong>{formData.professor}</strong>
                  </div>

                  <div>
                    <span>ACTIVITY DATE</span>
                    <strong>{formData.activityDate}</strong>
                  </div>

                  <div>
                    <span>LOCATION</span>
                    <strong>{formData.location}</strong>
                  </div>

                  <div>
                    <span>SUBMISSION DEADLINE</span>
                    <strong>{formData.deadline}</strong>
                  </div>
                </div>

                <h3>Activity Description</h3>
                <p className="create-consent-document-text">
                  {formData.description}
                </p>

                <h3>Additional Instructions</h3>
                <p className="create-consent-document-text">
                  {formData.instructions || "No additional instructions."}
                </p>

                <h3>Selected Students ({selectedStudents.length})</h3>

                <ul className="create-consent-preview-students">
                  {selectedStudents.map((student) => (
                    <li key={student.id}>
                      {student.fullName} ({student.studentNumber})
                    </li>
                  ))}
                </ul>

                <div className="create-consent-document-notice">
                  Parent or guardian consent will be collected through the
                  digital consent submission page. Parents or guardians will
                  be asked to provide their printed name, consent decision,
                  and signature.
                </div>
              </div>

              <div className="create-consent-document-footer">
                University of the Assumption · Digital Parent Consent System
              </div>
            </article>

            <div className="create-consent-publish-note">
              <span>✓</span>
              <p>
                {isEditMode
                  ? "Review your changes before updating this draft."
                  : "You can save this form as a draft for later or publish it to make it available to parents."}
              </p>
            </div>

            <div className="create-consent-actions">
              <button
                type="button"
                className="create-consent-secondary"
                onClick={() => setShowPreview(false)}
                disabled={publishing}
              >
                ← Edit Form
              </button>

              {isEditMode ? (
                <button
                  type="button"
                  className="create-consent-primary"
                  onClick={() => handleSave("Draft")}
                  disabled={publishing}
                >
                  {publishing ? "Updating..." : "Update Draft"}
                </button>
              ) : (
                <>
                  <button
                    type="button"
                    className="create-consent-secondary"
                    onClick={() => handleSave("Draft")}
                    disabled={publishing}
                  >
                    {publishing ? "Saving..." : "Save as Draft"}
                  </button>

                  <button
                    type="button"
                    className="create-consent-primary"
                    onClick={() => handleSave("Published")}
                    disabled={publishing}
                  >
                    {publishing ? "Saving..." : "Publish Consent Form"}
                  </button>
                </>
              )}
            </div>
          </section>
        )}
      </main>
    </div>
  );
}

export default CreateConsentForm;
