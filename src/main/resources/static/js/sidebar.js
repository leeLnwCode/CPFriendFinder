document.addEventListener("DOMContentLoaded", async () => {

    const profileImage = document.getElementById("sidebarProfileImage");
    const profileName = document.getElementById("sidebarProfileName");
    const profileYear = document.getElementById("sidebarProfileYear");
    const profileDepartment = document.getElementById("sidebarProfileDepartment");

    // ถ้าหน้านี้ไม่มี Sidebar ก็ไม่ต้องทำอะไร
    if (!profileName) {
        return;
    }

    try {
        const response = await fetch("/api/users/me", {
            method: "GET",
            headers: {
                Accept: "application/json"
            },
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error(`โหลดข้อมูลผู้ใช้ไม่สำเร็จ (${response.status})`);
        }

        const user = await response.json();

        // ชื่อ
        const firstname = user.firstname || "";
        const lastname = user.lastname || "";

        const fullname = `${firstname} ${lastname}`.trim();

        profileName.textContent = fullname || "ไม่ระบุชื่อ";

        // ปี
        if (user.year !== null && user.year !== undefined) {
            profileYear.textContent = `ปี ${user.year}`;
        } else {
            profileYear.textContent = "";
        }

        // Department
        profileDepartment.textContent = user.department || "";

        // รูปโปรไฟล์
        if (user.image_url) {
            profileImage.src = user.image_url;
        }

    } catch (error) {

        console.error("โหลดข้อมูล Sidebar ไม่สำเร็จ:", error);

        profileName.textContent = "ไม่สามารถโหลดข้อมูล";
        profileYear.textContent = "";
        profileDepartment.textContent = "";
    }
});