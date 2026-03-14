export function renderSidebar(activePage, user) {
  const role = user.role;
  const nav = [
    { id: "dashboard", icon: "🏠", label: "Dashboard", href: "dashboard.html" },
    { id: "students", icon: "👥", label: "Students", href: "students.html" },
    { id: "question-bank", icon: "📚", label: "Question Bank", href: "question-bank.html" },
    { id: "exams", icon: "📝", label: "Exams", href: "exams.html" },
    { id: "summary", icon: "📊", label: "Summary", href: "summary.html" }
  ];

  if (role === "admin") {
    nav.push({ id: "users", icon: "🛡️", label: "User Management", href: "users.html" });
  }

  const navHTML = nav.map(item => `
    <a href="${item.href}" class="nav-item ${activePage === item.id ? "active" : ""}">
      <span class="nav-icon">${item.icon}</span>
      <span>${item.label}</span>
    </a>
  `).join("");

  return `
    <aside class="sidebar" id="sidebar">
      <div class="sidebar-logo">
        <div class="sidebar-logo-icon">🎓</div>
        <div class="sidebar-logo-text">
          <h4>PBEC Portal</h4>
          <span>Teacher & Admin</span>
        </div>
      </div>

      <div class="sidebar-section">
        <div class="sidebar-section-label">Navigation</div>
        ${navHTML}
      </div>

      <div class="sidebar-footer">
        <div class="sidebar-user">
          <div class="sidebar-avatar" id="user-avatar">?</div>
          <div class="sidebar-user-info">
            <div class="sidebar-user-name truncate" id="user-email-display">Loading…</div>
            <div class="sidebar-user-role">${role === "admin" ? "Admin" : "Teacher"} ${user?.teacherId ? `• ${user.teacherId}` : ""}</div>
          </div>
          <button class="sidebar-logout" id="logout-btn" title="Sign out">⏏</button>
        </div>
      </div>
    </aside>
    <div id="sidebar-overlay" class="hidden" style="
      position:fixed;inset:0;background:rgba(0,0,0,0.5);z-index:200;
    "></div>
  `;
}
