import {
    BrowserRouter,
    Routes,
    Route,
    Navigate
} from "react-router-dom";

import { useEffect, useState } from "react";
import "./styles/theme.css";
import Login from "./pages/login";
import Register from "./pages/register";

function App() {

    const [theme, setTheme] = useState(
        localStorage.getItem("theme") || "black"
    );

    useEffect(() => {
        document.documentElement.setAttribute(
            "data-theme",
            theme
        );

        localStorage.setItem("theme", theme);
    }, [theme]);
    
    return (
        <BrowserRouter>

        {/* ปุ่มทดลองเปลี่ยนธีม */}
            <div className="theme-test">

                <button onClick={() => setTheme("white")}>
                    ขาว
                </button>

                <button onClick={() => setTheme("black")}>
                    ดำ
                </button>

                <button onClick={() => setTheme("blue")}>
                    ฟ้า
                </button>

            </div>
            
            <Routes>

                <Route
                    path="/"
                    element={<Navigate to="/login" />}
                />

                <Route
                    path="/login"
                    element={<Login />}
                />

                <Route
                    path="/register"
                    element={<Register />}
                />

            </Routes>
        </BrowserRouter>
    );
}

export default App;