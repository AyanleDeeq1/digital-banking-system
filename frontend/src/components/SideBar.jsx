import { NavLink } from "react-router-dom";
import "../style/SideBar.css";

function SideBar() {
    return (
        <aside className="sidebar">

            <nav className="sidebar-nav">

                <NavLink
                    to="/dashboard"
                    className={({ isActive }) => isActive ? "active" : ""}
                >
                    Dashboard
                </NavLink>

                <NavLink to="/accounts">
                    Accounts
                </NavLink>

                <NavLink to="/my-card">
                    My Card
                </NavLink>

            </nav>
              <div className="sidebar-bottom">
                <button className="sidebar-logout">
                    Logout
                </button>
            </div>

        </aside>
    );
}

export default SideBar;
