import { NavLink } from "react-router-dom";
import "../style/SideBar.css";


function SideBar({logout}) {
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
                <button className="sidebar-logout" type="button" onClick={logout?.submit} disabled={!logout || logout.pending}>
                    {logout?.pending ? 'Logging out…' : 'Logout'}
                </button>
                {logout?.error && <p role="alert">{logout.error}</p>}
            </div>

        </aside>
    );
}

export default SideBar;
