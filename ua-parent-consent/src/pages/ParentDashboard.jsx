
import { useEffect, useState } from "react";
import Sidebar from "../components/Sidebar";
import { apiRequest } from "../services/api";

function ParentDashboard() {
  const [consentForms, setConsentForms] = useState([]);
  const [studentLinks, setStudentLinks] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [linksLoading, setLinksLoading] = useState(true);
  const [linkError, setLinkError] = useState("");

  const [studentNumber, setStudentNumber] = useState("");
  const [relationship, setRelationship] = useState("");
  const [submittingLink, setSubmittingLink] = useState(false);
  const [linkMessage, setLinkMessage] = useState("");

  // Notifications
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notificationsLoading, setNotificationsLoading] = useState(true);
  const [notificationError, setNotificationError] = useState("");
  const [showNotifications, setShowNotifications] = useState(false);
  const [markingAllRead, setMarkingAllRead] = useState(false);

  const loggedInUser = JSON.parse(
    localStorage.getItem("loggedInUser") || "null"
  );

  const parentName = loggedInUser?.fullName || "Parent / Guardian";
  const firstName = parentName.split(" ")[0];

  const initials = parentName
    .split(" ")
    .filter(Boolean)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  // Load consent forms and responses.
  useEffect(() => {
    let active = true;

    const fetchConsentForms = async () => {
      try {
        setLoading(true);
        setError("");

        const [formsData, responsesData] = await Promise.all([
          apiRequest("/parent/consent-forms"),
          apiRequest("/parent/consent-responses"),
        ]);

        const forms = Array.isArray(formsData) ? formsData : [];
        const responses = Array.isArray(responsesData)
          ? responsesData
          : [];

        const updatedForms = forms.map((form) => {
          const response = responses.find(
            (item) =>
              String(item.formId) === String(form.id) &&
              item.studentId != null &&
              String(item.studentId) === String(form.studentId)
          );

          return {
            ...form,
            status: response
              ? `Submitted - ${response.decision}`
              : "Pending",
          };
        });

        if (active) {
          setConsentForms(updatedForms);
        }
      } catch (err) {
        console.error("Parent dashboard loading error:", err);

        if (active) {
          setError(err.message || "Unable to load consent forms.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    fetchConsentForms();

    return () => {
      active = false;
    };
  }, []);

  // Load notifications and unread count.
  const fetchNotifications = async () => {
    try {
      setNotificationsLoading(true);
      setNotificationError("");

      const [notificationsData, countData] = await Promise.all([
        apiRequest("/notifications"),
        apiRequest("/notifications/unread-count"),
      ]);

      setNotifications(
        Array.isArray(notificationsData) ? notificationsData : []
      );

      setUnreadCount(Number(countData?.unreadCount || 0));
    } catch (err) {
      console.error("Notification loading error:", err);
      setNotificationError(
        err.message || "Unable to load notifications."
      );
    } finally {
      setNotificationsLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  // Mark one notification as read.
  const handleMarkAsRead = async (notificationId) => {
    try {
      const updatedNotification = await apiRequest(
        `/notifications/${notificationId}/read`,
        {
          method: "PUT",
        }
      );

      setNotifications((previous) =>
        previous.map((notification) =>
          notification.id === notificationId
            ? updatedNotification
            : notification
        )
      );

      setUnreadCount((previous) => Math.max(0, previous - 1));
    } catch (err) {
      console.error("Mark notification as read error:", err);
      setNotificationError(
        err.message || "Unable to update notification."
      );
    }
  };

  // Mark all notifications as read.
  const handleMarkAllAsRead = async () => {
    if (markingAllRead || unreadCount === 0) return;

    try {
      setMarkingAllRead(true);
      setNotificationError("");

      await apiRequest("/notifications/read-all", {
        method: "PUT",
      });

      setNotifications((previous) =>
        previous.map((notification) => ({
          ...notification,
          read: true,
        }))
      );

      setUnreadCount(0);
    } catch (err) {
      console.error("Mark all notifications as read error:", err);
      setNotificationError(
        err.message || "Unable to update notifications."
      );
    } finally {
      setMarkingAllRead(false);
    }
  };

  // Load parent-student links.
  const fetchStudentLinks = async () => {
    try {
      setLinksLoading(true);
      setLinkError("");

      const data = await apiRequest("/parent-student-links/my-links");

      setStudentLinks(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Student links loading error:", err);
      setLinkError(err.message || "Unable to load linked students.");
    } finally {
      setLinksLoading(false);
    }
  };

  useEffect(() => {
    fetchStudentLinks();
  }, []);

  // Submit a student-link request.
  const handleLinkSubmit = async (e) => {
    e.preventDefault();

    if (submittingLink) return;

    const cleanStudentNumber = studentNumber.trim();

    if (!cleanStudentNumber) {
      setLinkMessage("Please enter the student's number.");
      return;
    }

    if (!relationship) {
      setLinkMessage("Please select your relationship to the student.");
      return;
    }

    try {
      setSubmittingLink(true);
      setLinkMessage("");

      await apiRequest("/parent-student-links/request", {
        method: "POST",
        body: JSON.stringify({
          studentNumber: cleanStudentNumber,
          relationship,
        }),
      });

      setStudentNumber("");
      setRelationship("");
      setLinkMessage("Your student-link request was submitted successfully.");

      await fetchStudentLinks();
    } catch (err) {
      console.error("Student-link request error:", err);
      setLinkMessage(
        err.message || "Unable to submit your student-link request."
      );
    } finally {
      setSubmittingLink(false);
    }
  };

  // Summary statistics.
  const verifiedStudentCount = studentLinks.filter(
    (link) => link.verificationStatus === "VERIFIED"
  ).length;

  const pendingCount = consentForms.filter(
    (form) => form.status === "Pending"
  ).length;

  const submittedCount = consentForms.filter(
    (form) => form.status.startsWith("Submitted -")
  ).length;

  // Upcoming deadlines within the next 7 days.
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const nextSevenDays = new Date(today);
  nextSevenDays.setDate(today.getDate() + 7);

  const upcomingDeadlineCount = consentForms.filter((form) => {
    if (form.status !== "Pending" || !form.deadline) return false;

    const deadline = new Date(`${form.deadline}T00:00:00`);

    return (
      !Number.isNaN(deadline.getTime()) &&
      deadline >= today &&
      deadline <= nextSevenDays
    );
  }).length;

  // Status styling.
  const getStatusClass = (status) => {
    if (status === "Pending") return "parent-status-pending";
    if (status === "VERIFIED") return "parent-status-approved";
    if (status === "REJECTED") return "parent-status-declined";
    return "";
  };

  // Format dates.
  const formatDate = (date) => {
    if (!date) return "—";

    const parsedDate = /^\d{4}-\d{2}-\d{2}$/.test(date)
      ? new Date(`${date}T00:00:00`)
      : new Date(date);

    if (Number.isNaN(parsedDate.getTime())) return date;

    return parsedDate.toLocaleDateString("en-PH", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  // Format notification timestamps.
  const formatNotificationDate = (date) => {
    if (!date) return "";

    const parsedDate = new Date(date);

    if (Number.isNaN(parsedDate.getTime())) return "";

    return parsedDate.toLocaleString("en-PH", {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
    });
  };

  return (
    <div className="parent-layout">
      <Sidebar />

      <main className="parent-main parent-dashboard-redesign">

        {/* PAGE HEADER */}
        <header className="dashboard-header parent-new-header">
          <div>
            <span className="parent-page-label">PARENT PORTAL</span>
            <h1>Dashboard</h1>
            <p>
              Manage consent forms and stay connected with school activities.
            </p>
          </div>

          <div className="parent-header-actions">

            {/* NOTIFICATION BELL */}
            <div className="parent-notification-wrapper">
              <button
                type="button"
                className="parent-notification-button"
                onClick={() => {
                  setShowNotifications((previous) => !previous);

                  if (!showNotifications) {
                    fetchNotifications();
                  }
                }}
                aria-label={`Notifications, ${unreadCount} unread`}
                aria-expanded={showNotifications}
              >
                <span>🔔</span>

                {unreadCount > 0 && (
                  <span className="parent-notification-badge">
                    {unreadCount > 99 ? "99+" : unreadCount}
                  </span>
                )}
              </button>

              {showNotifications && (
                <div className="parent-notification-dropdown">

                  <div className="parent-notification-heading">
                    <div>
                      <h3>Notifications</h3>
                      <span>
                        {unreadCount} unread
                      </span>
                    </div>

                    <button
                      type="button"
                      onClick={handleMarkAllAsRead}
                      disabled={unreadCount === 0 || markingAllRead}
                    >
                      {markingAllRead ? "Updating..." : "Mark all as read"}
                    </button>
                  </div>

                  {notificationError && (
                    <div className="parent-notification-error">
                      {notificationError}
                      <button
                        type="button"
                        onClick={fetchNotifications}
                      >
                        Retry
                      </button>
                    </div>
                  )}

                  {notificationsLoading ? (
                    <div className="parent-notification-empty">
                      Loading notifications...
                    </div>
                  ) : notifications.length > 0 ? (
                    <div className="parent-notification-list">
                      {notifications.map((notification) => (
                        <div
                          key={notification.id}
                          className={`parent-notification-item ${
                            notification.read
                              ? "is-read"
                              : "is-unread"
                          }`}
                        >
                          <div className="parent-notification-item-content">
                            <div className="parent-notification-title-row">
                              <h4>{notification.title}</h4>

                              {!notification.read && (
                                <span className="parent-notification-dot" />
                              )}
                            </div>

                            <p>{notification.message}</p>

                            <span className="parent-notification-date">
                              {formatNotificationDate(
                                notification.createdAt
                              )}
                            </span>
                          </div>

                          {!notification.read && (
                            <button
                              type="button"
                              className="parent-notification-read-button"
                              onClick={() =>
                                handleMarkAsRead(notification.id)
                              }
                            >
                              Mark as read
                            </button>
                          )}
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="parent-notification-empty">
                      <span>🔔</span>
                      <h4>No Notifications</h4>
                      <p>
                        You're all caught up. New updates will appear here.
                      </p>
                    </div>
                  )}

                </div>
              )}
            </div>

            <div className="parent-profile">
              <div className="profile-avatar">{initials || "P"}</div>
              <div>
                <h4>{parentName}</h4>
                <span>Parent / Guardian</span>
              </div>
            </div>
          </div>
        </header>

        {/* WELCOME BANNER */}
        <section className="parent-welcome-banner">
          <div className="parent-welcome-content">
            <span className="parent-welcome-label">
              UNIVERSITY OF THE ASSUMPTION
            </span>

            <h2>Welcome, {firstName}!</h2>

            <p>
              Your parent portal is ready. Review consent forms,
              submit your responses, and stay updated on upcoming
              university activities.
            </p>
          </div>

          <div className="parent-welcome-decoration" aria-hidden="true">
            <div className="parent-welcome-seal">UA</div>
          </div>
        </section>

        {/* SUMMARY */}
        <section className="parent-summary-section">
          <div className="parent-section-title">
            <div>
              <h2>Consent Overview</h2>
              <p>A quick summary of your parent portal activity.</p>
            </div>
          </div>

          <div className="summary-grid parent-summary-grid">

            <div className="summary-card parent-stat-card">
              <div className="parent-stat-icon approved-icon">👨‍👩‍👧</div>
              <div>
                <h3>
                  {linksLoading ? "..." : verifiedStudentCount}
                </h3>
                <p>Linked Students</p>
                <span>Verified student connections</span>
              </div>
            </div>

            <div className="summary-card parent-stat-card">
              <div className="parent-stat-icon pending-icon">📄</div>
              <div>
                <h3>{loading ? "..." : pendingCount}</h3>
                <p>Pending Forms</p>
                <span>Awaiting your response</span>
              </div>
            </div>

            <div className="summary-card parent-stat-card">
              <div className="parent-stat-icon approved-icon">✓</div>
              <div>
                <h3>{loading ? "..." : submittedCount}</h3>
                <p>Submitted Forms</p>
                <span>Responses already submitted</span>
              </div>
            </div>

            <div className="summary-card parent-stat-card">
              <div className="parent-stat-icon declined-icon">⏰</div>
              <div>
                <h3>{loading ? "..." : upcomingDeadlineCount}</h3>
                <p>Upcoming Deadlines</p>
                <span>Due within the next 7 days</span>
              </div>
            </div>

          </div>
        </section>

        {/* STUDENT LINK REQUEST */}
        <section className="content-card parent-new-card">
          <div className="section-heading">
            <div>
              <span className="parent-card-eyebrow">STUDENT CONNECTION</span>
              <h2>Link a Student</h2>
              <p>
                Submit a request to connect your parent account to a student.
                A teacher must verify your request.
              </p>
            </div>
          </div>

          <form
            className="parent-link-form"
            onSubmit={handleLinkSubmit}
          >
            <div className="parent-link-form-grid">
              <div className="parent-link-field">
                <label htmlFor="studentNumber">
                  Student Number <span>*</span>
                </label>
                <input
                  id="studentNumber"
                  type="text"
                  value={studentNumber}
                  onChange={(e) => setStudentNumber(e.target.value)}
                  placeholder="Enter student number"
                  maxLength={50}
                  required
                  disabled={submittingLink}
                />
              </div>

              <div className="parent-link-field">
                <label htmlFor="relationship">
                  Relationship to Student <span>*</span>
                </label>
                <select
                  id="relationship"
                  value={relationship}
                  onChange={(e) => setRelationship(e.target.value)}
                  required
                  disabled={submittingLink}
                >
                  <option value="">Select relationship</option>
                  <option value="Mother">Mother</option>
                  <option value="Father">Father</option>
                  <option value="Guardian">Guardian</option>
                  <option value="Other">Other</option>
                </select>
              </div>
            </div>

            {linkMessage && (
              <p className="parent-link-message" role="status">
                {linkMessage}
              </p>
            )}

            <button
              type="submit"
              className="parent-link-submit"
              disabled={submittingLink}
            >
              {submittingLink ? "Submitting..." : "Submit Link Request"}
            </button>
          </form>
        </section>

        {/* LINKED STUDENTS */}
        <section className="content-card parent-new-card">
          <div className="section-heading">
            <div>
              <span className="parent-card-eyebrow">YOUR CONNECTIONS</span>
              <h2>My Linked Students</h2>
              <p>
                View your student-link requests and their verification status.
              </p>
            </div>

            <span className="total-label">
              {studentLinks.length} Students
            </span>
          </div>

          {linksLoading ? (
            <div className="parent-empty-state">
              <h3>Loading Linked Students...</h3>
              <p>Please wait while we retrieve your requests.</p>
            </div>
          ) : linkError ? (
            <div className="parent-empty-state">
              <h3>Unable to Load Linked Students</h3>
              <p>{linkError}</p>
              <button type="button" onClick={fetchStudentLinks}>
                Try Again
              </button>
            </div>
          ) : studentLinks.length > 0 ? (
            <div className="table-container">
              <table className="parent-consent-table">
                <thead>
                  <tr>
                    <th>Student Name</th>
                    <th>Student Number</th>
                    <th>Section</th>
                    <th>Relationship</th>
                    <th>Verification Status</th>
                  </tr>
                </thead>

                <tbody>
                  {studentLinks.map((link) => (
                    <tr key={link.id}>
                      <td>
                        <strong>{link.studentName || "—"}</strong>
                      </td>
                      <td>{link.studentNumber || "—"}</td>
                      <td>{link.sectionName || "—"}</td>
                      <td>{link.relationship || "—"}</td>
                      <td>
                        <span
                          className={`parent-status-pill ${getStatusClass(
                            link.verificationStatus
                          )}`}
                        >
                          {link.verificationStatus || "Unknown"}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="parent-empty-state">
              <div className="parent-empty-icon">👨‍👩‍👧</div>
              <h3>No Linked Students Yet</h3>
              <p>
                Submit a student-link request using the form above.
                Your request will appear here for verification.
              </p>
            </div>
          )}
        </section>

        {/* CONSENT FORMS */}
        <section className="content-card parent-new-card">
          <div className="section-heading">
            <div>
              <span className="parent-card-eyebrow">YOUR ACTIVITIES</span>
              <h2>My Consent Forms</h2>
              <p>
                Review the consent forms published by your child's teacher.
              </p>
            </div>

            <span className="total-label">
              {consentForms.length} Forms
            </span>
          </div>

          {loading ? (
            <div className="parent-empty-state">
              <h3>Loading Consent Forms...</h3>
              <p>Please wait while we retrieve published forms.</p>
            </div>
          ) : error ? (
            <div className="parent-empty-state">
              <h3>Unable to Load Consent Forms</h3>
              <p>{error}</p>
              <button
                type="button"
                onClick={() => window.location.reload()}
              >
                Try Again
              </button>
            </div>
          ) : consentForms.length > 0 ? (
            <div className="table-container">
              <table className="parent-consent-table">
                <thead>
                  <tr>
                    <th>Activity</th>
                    <th>Class</th>
                    <th>Activity Date</th>
                    <th>Deadline</th>
                    <th>Status</th>
                  </tr>
                </thead>

                <tbody>
                  {consentForms.map((form) => (
                    <tr key={`${form.id}-${form.studentId}`}>
                      <td>
                        <strong>{form.title}</strong>
                      </td>
                      <td>{form.className || "—"}</td>
                      <td>{formatDate(form.activityDate)}</td>
                      <td>{formatDate(form.deadline)}</td>
                      <td>
                        <span
                          className={`parent-status-pill ${getStatusClass(
                            form.status
                          )}`}
                        >
                          {form.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="parent-empty-state">
              <div className="parent-empty-icon">📄</div>
              <h3>No Consent Forms Yet</h3>
              <p>
                There are currently no published consent forms.
                Please check again later.
              </p>
            </div>
          )}
        </section>

        {/* FOOTER */}
        <footer className="parent-dashboard-footer">
          University of the Assumption · E-ParentConsent Portal
        </footer>

      </main>
    </div>
  );
}

export default ParentDashboard;
