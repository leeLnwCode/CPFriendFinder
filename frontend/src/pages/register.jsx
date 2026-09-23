import { useState } from "react";
import { Link } from "react-router-dom";
import "../styles/register.css";

import battle from "../assets/battle.png";

function Register() {

    const [step, setStep] = useState(1);

    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const [selectedInterests, setSelectedInterests] = useState([]);

    const interests = [
        "Game",
        "Music",
        "Sport",
        "Study",
        "Technology",
        "Movie",
        "Anime",
        "Travel",
        "Project",
        "Hackathon",
        "Other"
    ];

    const toggleInterest = (interest) => {

        if (selectedInterests.includes(interest)) {

            setSelectedInterests(
                selectedInterests.filter(
                    item => item !== interest
                )
            );

        } else {

            setSelectedInterests([
                ...selectedInterests,
                interest
            ]);

        }
    };


    return (
        <div className="register-page">

            <div className="register-card">




                {/* =========================
                    STEP 1 : ACCOUNT
                ========================= */}

                {step === 1 && (

                    <>
                        <h1>สร้างบัญชี</h1>

                        <p className="register-subtitle">
                            สร้างบัญชีของคุณเพื่อหาเพื่อนใหม่!
                        </p>


                        <form className="register-form">

                            <label>Email address</label>

                            <input
                                type="email"
                                placeholder="Enter your email"
                            />


                            <label>Password</label>

                            <div className="password-wrapper">

                                <input
                                    type={
                                        showPassword
                                            ? "text"
                                            : "password"
                                    }
                                    placeholder="Create your password"
                                />

                                <button
                                    type="button"
                                    onClick={() =>
                                        setShowPassword(
                                            !showPassword
                                        )
                                    }
                                >
                                    {showPassword
                                        ? "Hide"
                                        : "Show"}
                                </button>

                            </div>


                            <label>Confirm Password</label>

                            <div className="password-wrapper">

                                <input
                                    type={
                                        showConfirmPassword
                                            ? "text"
                                            : "password"
                                    }
                                    placeholder="Confirm your password"
                                />

                                <button
                                    type="button"
                                    onClick={() =>
                                        setShowConfirmPassword(
                                            !showConfirmPassword
                                        )
                                    }
                                >
                                    {showConfirmPassword
                                        ? "Hide"
                                        : "Show"}
                                </button>

                            </div>

                        </form>


                        <div className="register-buttons">

                            <Link
                                to="/login"
                                className="cancel-button"
                            >
                                ยกเลิก
                            </Link>

                            <button
                                type="button"
                                className="next-button"
                                onClick={() => setStep(2)}
                            >
                                ถัดไป
                            </button>

                        </div>


                        <div className="already-account">
                            Already have an account?

                            <Link to="/login">
                                Login
                            </Link>
                        </div>

                    </>
                )}


                {/* =========================
                    STEP 2 : PROFILE
                ========================= */}

                {step === 2 && (

                    <>
                        <h1>สร้าง Profile</h1>

                        <p className="register-subtitle">
                            บอกข้อมูลเกี่ยวกับตัวคุณ
                        </p>


                        <form className="register-form">

                            {/* Profile Picture */}

                            <div className="profile-upload">

                                <div className="profile-circle">
                                    +
                                </div>

                                <button
                                    type="button"
                                    className="upload-button"
                                >
                                    Upload Profile Picture
                                </button>

                            </div>


                            <label>Username</label>

                            <input
                                type="text"
                                placeholder="Enter your username"
                            />


                            <div className="name-row">

                                <div>
                                    <label>First name</label>

                                    <input
                                        type="text"
                                        placeholder="First name"
                                    />
                                </div>


                                <div>
                                    <label>Last name</label>

                                    <input
                                        type="text"
                                        placeholder="Last name"
                                    />
                                </div>

                            </div>


                            <label>Date of birth</label>

                            <input type="date" />


                            <label>Study year</label>

                            <select>

                                <option value="">
                                    Select your year
                                </option>

                                <option value="1">
                                    ปี 1
                                </option>

                                <option value="2">
                                    ปี 2
                                </option>

                                <option value="3">
                                    ปี 3
                                </option>

                                <option value="4">
                                    ปี 4
                                </option>

                            </select>


                            <label>Department</label>

                            <select>

                                <option value="">
                                    Select your Department
                                </option>

                                <option value="CS">
                                    CS
                                </option>

                                <option value="CS-VIP">
                                    CS-VIP
                                </option>

                                <option value="3ITII">
                                    ITII
                                </option>

                                <option value="CY">
                                    CY
                                </option>

                                <option value="CY-VIP">
                                    CY-VIP
                                </option>

                                <option value="AI">
                                    AI
                                </option>

                                <option value="AI-VIP">
                                    AI-VIP
                                </option>

                                <option value="GIS">
                                    GIS
                                </option>

                            </select>

                        </form>


                        <div className="register-buttons">

                            <button
                                type="button"
                                className="cancel-button"
                                onClick={() => setStep(1)}
                            >
                                ย้อนกลับ
                            </button>

                            <button
                                type="button"
                                className="next-button"
                                onClick={() => setStep(3)}
                            >
                                ถัดไป
                            </button>

                        </div>

                    </>
                )}


                {/* =========================
                    STEP 3 : INTEREST
                ========================= */}

                {step === 3 && (

                    <>
                        <h1>เลือกความสนใจของคุณ</h1>

                        <p className="register-subtitle">
                            เลือกสิ่งที่คุณสนใจเพื่อหาเพื่อนที่เข้ากันได้
                        </p>

                        <div className="interest-container">

                            {/* เกม */}
                            <div className="interest-group">

                                <h3>เกม</h3>

                                <div className="interest-list">

                                    {[
                                        "🎮 Gaming",
                                        "⚔ ROV",
                                        "⚔ Minecraft",
                                        "⚔ Valorant",
                                        "⚔ เกมอื่น ๆ"
                                    ].map((interest) => (

                                        <button
                                            key={interest}
                                            type="button"
                                            className={
                                                selectedInterests.includes(interest)
                                                    ? "interest selected"
                                                    : "interest"
                                            }
                                            onClick={() =>
                                                toggleInterest(interest)
                                            }
                                        >
                                            {interest}
                                        </button>

                                    ))}

                                </div>

                            </div>


                            {/* กิจกรรม */}
                            <div className="interest-group">

                                <h3>กิจกรรม</h3>

                                <div className="interest-list">

                                    {[
                                        "</> Programming",
                                        "</> Project",
                                        "</> Hackathon"
                                    ].map((interest) => (

                                        <button
                                            key={interest}
                                            type="button"
                                            className={
                                                selectedInterests.includes(interest)
                                                    ? "interest selected"
                                                    : "interest"
                                            }
                                            onClick={() =>
                                                toggleInterest(interest)
                                            }
                                        >
                                            {interest}
                                        </button>

                                    ))}

                                </div>

                            </div>


                            {/* ความบันเทิง */}
                            <div className="interest-group">

                                <h3>ความบันเทิง</h3>

                                <div className="interest-list">

                                    {[
                                        "</> Anime",
                                        "</> Movie",
                                        "</> Book"
                                    ].map((interest) => (

                                        <button
                                            key={interest}
                                            type="button"
                                            className={
                                                selectedInterests.includes(interest)
                                                    ? "interest selected"
                                                    : "interest"
                                            }
                                            onClick={() =>
                                                toggleInterest(interest)
                                            }
                                        >
                                            {interest}
                                        </button>

                                    ))}

                                </div>

                            </div>

                        </div>


                        <div className="register-buttons">

                            <button
                                type="button"
                                className="cancel-button"
                                onClick={() => setStep(2)}
                            >
                                ย้อนกลับ
                            </button>

                            <button
                                type="button"
                                className="next-button"
                            >
                                สร้างบัญชี
                            </button>

                        </div>

                    </>
                )}

            </div>

        </div>
    );
}

export default Register;