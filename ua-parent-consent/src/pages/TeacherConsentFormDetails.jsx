
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import TeacherSidebar from "../components/TeacherSidebar";
import { apiRequest } from "../services/api";

function TeacherConsentFormDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [form, setForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [publishing, setPublishing] = useState(false);
  const [error, setError] = useState("");
  const [publishError, setPublishError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    const fetchForm = async () => {
      try {
        setLoading(true);
        setError("");

        const data = await apiRequest(
          `/teacher/consent-forms/${id}`
        );

        setForm(data);
      } catch (err) {
        setError(err.message || "Unable to load consent form.");
      } finally {
        setLoading(false);
      }
    };

    fetchForm();
  }, [id]);

  const handlePublish = async () => {
    const confirmed = window.confirm(
      "Are you sure you want to publish this consent form? Verified parents of the assigned students will be notified."
    );

    if (!confirmed) return;

    try {
      setPublishing(true);
      setPublishError("");
      setSuccessMessage("");

      const data = await apiRequest(
        `/teacher/consent-forms/${id}/publish`,
        {
          method: "PATCH"
        }
      );

      setForm((previousForm) => ({
        ...previousForm,
        ...data,
        studentIds: previousForm?.studentIds ?? data.studentIds ?? []
      }));

      setSuccessMessage(
        "Consent form published successfully! Verified parents will be notified."
      );
    } catch (err) {
      setPublishError(
        err.message || "Unable to publish consent form."
      );
    } finally {
      setPublishing(false);
    }
  };

  return (
    <div className="parent-layout">
      <TeacherSidebar />

      <main className="parent-main create-consent-page">
        <header className="dashboard-header create-consent-header">
          <div>
            <span className="create-consent-eyebrow">
              TEACHER WORKSPACE
            </span>

            <h1>Consent Form Details</h1>

            <p>
              View the information saved in this consent form.
            </p>
          </div>

          <Link
            to="/teacher-consent-forms"
            className="create-consent-cancel"
          >
            Back to Consent Forms
          </Link>
        </header>

        <section className="create-consent-card">
          {loading ? (
            <p>Loading consent form...</p>
          ) : error ? (
            <p className="create-consent-student-error">
              {error}
            </p>
          ) : form ? (
            <>
              {/* Consent Form Header */}
              <div className="create-consent-card-heading">
                <div className="create-consent-heading-icon">
                  ▤
                </div>

                <div>
                  <h2>{form.title}</h2>

                  <p>
                    Status: <strong>{form.status}</strong>
                  </p>
                </div>
              </div>

              {/* Success Message */}
              {successMessage && (
                <p className="publish-success-message">
                  {successMessage}
                </p>
              )}

              {/* Error Message */}
              {publishError && (
                <p className="publish-error-message">
                  {publishError}
                </p>
              )}

              {/* Basic Information */}
              <div className="create-consent-section-title">
                <h3>Basic Information</h3>
              </div>

              <p>
                <strong>Description:</strong> {form.description}
              </p>

              <p>
                <strong>Class / Section:</strong> {form.className}
              </p>

              <p>
                <strong>Course:</strong> {form.course}
              </p>

              <p>
                <strong>Professor:</strong> {form.professor}
              </p>

              {/* Activity Schedule */}
              <div className="create-consent-section-title create-consent-second-section">
                <h3>Activity Schedule and Location</h3>
              </div>

              <p>
                <strong>Activity Date:</strong> {form.activityDate}
              </p>

              <p>
                <strong>Deadline:</strong> {form.deadline}
              </p>

              <p>
                <strong>Location:</strong> {form.location}
              </p>

              {/* Additional Information */}
              <div className="create-consent-section-title create-consent-second-section">
                <h3>Additional Information</h3>
              </div>

              <p>
                <strong>Instructions:</strong>{" "}
                {form.instructions || "No additional instructions."}
              </p>

              {/* Assigned Students */}
              <div className="create-consent-section-title create-consent-second-section">
                <h3>Assigned Students</h3>
              </div>

              {form.studentIds?.length > 0 ? (
                <ul>
                  {form.studentIds.map((studentId) => (
                    <li key={studentId}>
                      Student ID: {studentId}
                    </li>
                  ))}
                </ul>
              ) : (
                <p>No assigned students found.</p>
              )}

              {/* Draft Actions */}
              {form.status?.toLowerCase() === "draft" && (
                <div className="publish-draft-container">
                  <button
                    type="button"
                    className="create-consent-secondary"
                    onClick={() =>
                      navigate(`/teacher-edit-consent-form/${id}`)
                    }
                  >
                    Edit Draft
                  </button>

                  <button
                    type="button"
                    onClick={handlePublish}
                    disabled={publishing}
                    className="create-consent-submit"
                  >
                    {publishing ? "Publishing..." : "Publish Draft"}
                  </button>
                </div>
              )}
            </>
          ) : (
            <p>Consent form not found.</p>
          )}
        </section>
      </main>
    </div>
  );
}

export default TeacherConsentFormDetails;
