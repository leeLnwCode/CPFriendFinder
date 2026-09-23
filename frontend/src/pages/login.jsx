import { useState } from "react";
import { Link } from "react-router-dom";
import "../styles/login.css";

import logo from "../assets/Full_logo.png";
import loginImage from "../assets/login.jpg";
import lock from "../assets/padlock.png";
import emailImage from "../assets/email.png";

function Login() {
    const [showPassword, setShowPassword] = useState(false);

    return (
        <div className="login-page">

            <div className="login-section">

                <img
                    src={logo}
                    alt="CP Friend Finder"
                    className="login-logo"
                />

                <h1>Welcome Back!</h1>

                <p className="login-subtitle">
                    Login to find your friends
                </p>

                <form className="login-form">

                    <label>Email address</label>

                    <div className="input-wrapper">
                        <img
                            src={emailImage}
                            alt=""
                            className="input-icon"
                        />

                        <input
                            type="email"
                            placeholder="Enter your email"
                        />
                    </div>


                    <label>Password</label>

                    <div className="input-wrapper">
                        <img
                            src={lock}
                            alt=""
                            className="input-icon"
                        />

                        <input
                            type={showPassword ? "text" : "password"}
                            placeholder="Enter your password"
                        />

                        <button
                            type="button"
                            className="show-password"
                            onClick={() => setShowPassword(!showPassword)}
                        >
                            {showPassword ? "Hide" : "Show"}
                        </button>
                    </div>


                    <button
                        type="submit"
                        className="login-button"
                    >
                        Login now
                    </button>

                </form>


                <div className="or-divider">
                    <span>OR</span>
                </div>


                <Link
                    to="/register"
                    className="signup-button"
                >
                    Signup now
                </Link>

            </div>

            <div className="login-image-section">

                <img
                    src={loginImage}
                    alt="Friend Finder"
                />

            </div>

        </div>
    );
}

export default Login;