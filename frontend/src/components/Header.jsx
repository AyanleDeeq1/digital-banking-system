import { Link } from "react-router-dom";
import logo from '../assets/logo.png'
import '../style/Header.css'
import Button from "./Button.jsx";

function Header({customer, page}) {
  
    return (
        <header>
            <div className="header-logo">
                <Link to="/">
                    <img src={logo} alt="UrBank" />
                </Link>
            </div>

            <nav className="header-nav">

                    {!customer && page === "home" && (
                        <>
                        <Link to="/login">Login</Link>
                        <Link to="/register">Register</Link>
                        </>
                    ) }

                    {!customer && page==="register" &&
                    
                        <>
                            <Link to="/">Home</Link>
                            <Link to="/login">Login</Link>
                        </>
                    
                    }

                    {!customer && page==="login" &&
                        <>
                        <Link to="/">Home</Link>
                        <Link to="/register">Register</Link>
                        </>
                    }
                    {customer && (
                        <>
                            {page === "home" && (
                                <>
                                    <Link to="/dashboard">Dashboard</Link>
                                    <Link to="/accounts">Accounts</Link>
                                     <Button variant="logout">
                                             Logout
                                     </Button>
                                     
                                </>
                            )}
                            <Link to="/profile" className="customer-avatar" aria-label="Open profile"
                                aria-current={page === "profile" ? "page" : undefined}>
                                {customer.firstName?.charAt(0).toUpperCase()}
                                {customer.lastName?.charAt(0).toUpperCase()}
                            </Link>
                        </>
                    )}
            </nav>
        </header>
    )
}

export default Header
