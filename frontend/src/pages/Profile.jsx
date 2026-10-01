import { Link } from "react-router-dom";
import Header from "../components/Header.jsx";
import SideBar from "../components/SideBar.jsx";
import Footer from "../components/Footer.jsx";
import "../style/Profile.css";

function Profile({ customer, customerStatus }) {
    return (
        <div className="profile-page">
            <Header customer={customer} page="profile" />
            <div className="profile-body">
                <SideBar />
                <main className="profile-main">
                    <h1>Profile</h1>
                    <p className="profile-intro">Your personal details, all in one place.</p>
                    {customer ? (
                        <section className="profile-details" aria-label="Personal details">
                            <div className="profile-identity">
                                <div className="profile-avatar" aria-hidden="true">
                                    {customer.firstName?.charAt(0).toUpperCase()}
                                    {customer.lastName?.charAt(0).toUpperCase()}
                                </div>
                                <h2>{customer.firstName} {customer.lastName}</h2>
                            </div>
                            <dl>
                                <div><dt>First name</dt><dd>{customer.firstName || "Not available"}</dd></div>
                                <div><dt>Last name</dt><dd>{customer.lastName || "Not available"}</dd></div>
                                <div><dt>Email address</dt><dd>{customer.email || "Not available"}</dd></div>
                            </dl>
                        </section>
                    ) : customerStatus === "loading" ? (
                        <p role="status">Loading your profile...</p>
                    ) : customerStatus === "error" ? (
                        <p role="alert">Unable to load your profile. Please refresh to try again.</p>
                    ) : (
                        <p>Please <Link to="/login">log in</Link> to view your profile.</p>
                    )}
                </main>
            </div>
            <Footer />
        </div>
    );
}

export default Profile;
