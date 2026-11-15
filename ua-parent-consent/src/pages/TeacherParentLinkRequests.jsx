
import { useCallback, useEffect, useState } from "react";
import TeacherSidebar from "../components/TeacherSidebar";
import { apiRequest } from "../services/api";

function TeacherParentLinkRequests() {
  const [requests, setRequests] = useState([]);
  const [reviewedRequests, setReviewedRequests] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [processingId, setProcessingId] = useState(null);

  const fetchRequests = useCallback(async () => {
    try {
      setLoading(true);
      setError("");

      const [pendingData, reviewedData] = await Promise.all([
        apiRequest("/parent-student-links/pending"),
        apiRequest("/parent-student-links/reviewed"),
      ]);

      setRequests(Array.isArray(pendingData) ? pendingData : []);
      setReviewedRequests(
        Array.isArray(reviewedData) ? reviewedData : []
      );
    } catch (err) {
      setError(err.message || "Failed to load requests.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRequests();
  }, [fetchRequests]);

  const handleReview = async (linkId, decision) => {
    const action = decision === "APPROVE" ? "approve" : "reject";

    if (!window.confirm(`Are you sure you want to ${action} this request?`)) {
      return;
    }

    try {
      setProcessingId(linkId);
      setError("");
      setMessage("");

      await apiRequest(`/parent-student-links/${linkId}/review`, {
        method: "PATCH",
        body: JSON.stringify({ decision }),
      });

      setMessage(
        `Request successfully ${decision === "APPROVE" ? "approved" : "rejected"}.`
      );

      await fetchRequests();
    } catch (err) {
      setError(err.message || "Unable to review request.");
    } finally {
      setProcessingId(null);
    }
  };

  const formatDate = (date) =>
    date ? new Date(date).toLocaleString() : "—";

  const renderRequestDetails = (request) => (
    <div className="teacher-request-details">
      <div>
        <span>Student Name</span>
        <strong>{request.studentName || "—"}</strong>
      </div>

      <div>
        <span>Student Number</span>
        <strong>{request.studentNumber || "—"}</strong>
      </div>

      <div>
        <span>Class / Section</span>
        <strong>{request.sectionName || "—"}</strong>
      </div>

      <div>
        <span>Request Date</span>
        <strong>{formatDate(request.createdAt)}</strong>
      </div>
    </div>
  );

  return (
    <>
      <TeacherSidebar />

      <main className="teacher-page">
        <div className="teacher-page-header">
          <div>
            <h1>Parent–Student Link Requests</h1>
            <p>
              Review parent or guardian requests to link their
              accounts with students.
            </p>
          </div>

          <button
            type="button"
            onClick={fetchRequests}
            disabled={loading}
          >
            {loading ? "Refreshing..." : "Refresh"}
          </button>
        </div>

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

        {/* PENDING REQUESTS */}
        <div className="teacher-request-card">
          <div className="teacher-request-card-header">
            <div>
              <h2>Pending Requests</h2>
              <p>Requests awaiting your approval.</p>
            </div>

            <span className="teacher-request-count">
              {requests.length}
            </span>
          </div>

          {loading ? (
            <p>Loading requests...</p>
          ) : requests.length === 0 ? (
            <div className="teacher-empty-state">
              <h3>No Pending Requests</h3>
              <p>
                There are currently no parent-student link requests
                waiting for your approval.
              </p>
            </div>
          ) : (
            <div className="teacher-request-list">
              {requests.map((request) => (
                <div
                  className="teacher-request-item"
                  key={request.id}
                >
                  <div className="teacher-request-information">
                    <div>
                      <h3>
                        {request.parentName || "Unknown Parent"}
                      </h3>
                      <p>
                        Parent / Guardian
                        {request.relationship
                          ? ` • ${request.relationship}`
                          : ""}
                      </p>
                    </div>

                    <span className="teacher-pending-badge">
                      PENDING
                    </span>
                  </div>

                  {renderRequestDetails(request)}

                  <div className="teacher-request-actions">
                    <button
                      type="button"
                      className="teacher-approve-button"
                      disabled={processingId === request.id}
                      onClick={() =>
                        handleReview(request.id, "APPROVE")
                      }
                    >
                      {processingId === request.id
                        ? "Processing..."
                        : "Approve"}
                    </button>

                    <button
                      type="button"
                      className="teacher-reject-button"
                      disabled={processingId === request.id}
                      onClick={() =>
                        handleReview(request.id, "REJECT")
                      }
                    >
                      Reject
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* REVIEWED REQUESTS */}
        <div className="teacher-request-card teacher-reviewed-card">
          <div className="teacher-request-card-header">
            <div>
              <h2>Reviewed Requests</h2>
              <p>Previously approved and rejected requests.</p>
            </div>

            <span className="teacher-request-count">
              {reviewedRequests.length}
            </span>
          </div>

          {loading ? (
            <p>Loading reviewed requests...</p>
          ) : reviewedRequests.length === 0 ? (
            <div className="teacher-empty-state">
              <h3>No Reviewed Requests</h3>
              <p>
                Approved and rejected requests will appear here.
              </p>
            </div>
          ) : (
            <div className="teacher-request-list">
              {reviewedRequests.map((request) => (
                <div
                  className="teacher-request-item"
                  key={request.id}
                >
                  <div className="teacher-request-information">
                    <div>
                      <h3>
                        {request.parentName || "Unknown Parent"}
                      </h3>
                      <p>
                        Parent / Guardian
                        {request.relationship
                          ? ` • ${request.relationship}`
                          : ""}
                      </p>
                    </div>

                    <span
                      className={
                        request.verificationStatus === "VERIFIED"
                          ? "teacher-verified-badge"
                          : "teacher-rejected-badge"
                      }
                    >
                      {request.verificationStatus}
                    </span>
                  </div>

                  {renderRequestDetails(request)}

                  <div className="teacher-reviewed-information">
                    <div>
                      <span>Reviewed By</span>
                      <strong>
                        {request.verifiedByName || "—"}
                      </strong>
                    </div>

                    <div>
                      <span>Review Date</span>
                      <strong>
                        {formatDate(request.verifiedAt)}
                      </strong>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>
    </>
  );
}

export default TeacherParentLinkRequests;
