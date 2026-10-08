document.addEventListener("DOMContentLoaded", () => {

    const profileButton =
        document.getElementById("sidebarProfileButton");

    const profileModal =
        document.getElementById("profileModal");

    const profileModalOverlay =
        document.getElementById("profileModalOverlay");

    const closeProfileModal =
        document.getElementById("closeProfileModal");

    const cancelProfileEdit =
        document.getElementById("cancelProfileEdit");

    const saveProfileEdit =
        document.getElementById("saveProfileEdit");

    const changeProfileImage =
        document.getElementById("changeProfileImage");

    const profileImageInput =
        document.getElementById("profileImageInput");

    const editProfileImage =
        document.getElementById("editProfileImage");

    const editFirstname =
        document.getElementById("editFirstname");

    const editLastname =
        document.getElementById("editLastname");

    const editBio =
        document.getElementById("editBio");

    const editDateOfBirth =
        document.getElementById("editDateOfBirth");

    const editYear =
        document.getElementById("editYear");

    const editDepartment =
        document.getElementById("editDepartment");

    const sidebarProfileImage =
        document.getElementById("sidebarProfileImage");

    const sidebarProfileName =
        document.getElementById("sidebarProfileName");

    const sidebarProfileYear =
        document.getElementById("sidebarProfileYear");

    const sidebarProfileDepartment =
        document.getElementById("sidebarProfileDepartment");


    let currentUser = null;
    let selectedImageBase64 = null;


    // ========================================
    // เปิด Popup
    // ========================================

    async function openProfileModal() {

        try {

            const response = await fetch("/api/users/me", {
                method: "GET",
                headers: {
                    "Accept": "application/json"
                },
                credentials: "include"
            });

            if (!response.ok) {
                throw new Error("ไม่สามารถโหลดข้อมูลโปรไฟล์ได้");
            }

            currentUser = await response.json();

            // ใส่ข้อมูลลงใน Form
            editFirstname.value =
                currentUser.firstname || "";

            editLastname.value =
                currentUser.lastname || "";

            editBio.value =
                currentUser.bio || "";

            editDateOfBirth.value =
                currentUser.dateOfBirth || "";

            editYear.value =
                currentUser.year != null
                    ? String(currentUser.year)
                    : "";

            editDepartment.value =
                currentUser.department || "";


            // รูปโปรไฟล์
            if (currentUser.image_url) {
                editProfileImage.src =
                    currentUser.image_url;
            } else {
                editProfileImage.src =
                    "/images/man.jpg";
            }

            selectedImageBase64 = null;

            profileModal.classList.add("show");

        } catch (error) {

            console.error(
                "โหลดโปรไฟล์ไม่สำเร็จ:",
                error
            );

            alert("ไม่สามารถโหลดข้อมูลโปรไฟล์ได้");
        }
    }


    // ========================================
    // ปิด Popup
    // ========================================

    function closeModal() {
        profileModal.classList.remove("show");
        selectedImageBase64 = null;
    }


    // ========================================
    // เปลี่ยนรูป
    // ========================================

    changeProfileImage.addEventListener(
        "click",
        () => {
            profileImageInput.click();
        }
    );


    profileImageInput.addEventListener(
        "change",
        () => {

            const file =
                profileImageInput.files[0];

            if (!file) {
                return;
            }

            const reader =
                new FileReader();

            reader.onload = (event) => {

                selectedImageBase64 =
                    event.target.result;

                editProfileImage.src =
                    selectedImageBase64;
            };

            reader.readAsDataURL(file);
        }
    );


    // ========================================
    // บันทึก Profile
    // ========================================

    saveProfileEdit.addEventListener(
        "click",
        async () => {

            const firstname =
                editFirstname.value.trim();

            const lastname =
                editLastname.value.trim();

            const bio =
                editBio.value.trim();

            const dateOfBirth =
                editDateOfBirth.value;

            const department =
                editDepartment.value.trim();

            const yearValue =
                editYear.value;


            if (!firstname) {
                alert("กรุณากรอกชื่อ");
                return;
            }

            if (!lastname) {
                alert("กรุณากรอกนามสกุล");
                return;
            }


            const requestBody = {

                firstname: firstname,

                lastname: lastname,

                bio: bio,

                dateOfBirth: dateOfBirth || null,

                department: department,

                year: yearValue
                    ? Number(yearValue)
                    : null
            };


            // ถ้าเลือกภาพใหม่
            if (selectedImageBase64) {
                requestBody.imageBase64 =
                    selectedImageBase64;
            }


            try {

                saveProfileEdit.disabled = true;
                saveProfileEdit.textContent =
                    "กำลังบันทึก...";


                const response =
                    await fetch("/api/users/me", {

                        method: "POST",

                        headers: {
                            "Content-Type":
                                "application/json",

                            "Accept":
                                "application/json"
                        },

                        credentials: "include",

                        body:
                            JSON.stringify(requestBody)
                    });


                if (!response.ok) {

                    const errorText =
                        await response.text();

                    console.error(errorText);

                    throw new Error(
                        "บันทึกโปรไฟล์ไม่สำเร็จ"
                    );
                }


                const updatedUser =
                    await response.json();


                // อัปเดต Sidebar
                updateSidebar(updatedUser);


                // อัปเดตรูปใน Popup
                if (updatedUser.image_url) {
                    editProfileImage.src =
                        updatedUser.image_url;
                }


                alert("บันทึกโปรไฟล์เรียบร้อยแล้ว");

                closeModal();


            } catch (error) {

                console.error(
                    "บันทึกโปรไฟล์ไม่สำเร็จ:",
                    error
                );

                alert(
                    "ไม่สามารถบันทึกโปรไฟล์ได้"
                );

            } finally {

                saveProfileEdit.disabled = false;
                saveProfileEdit.textContent =
                    "บันทึก";
            }
        }
    );


    // ========================================
    // อัปเดต Sidebar
    // ========================================

    function updateSidebar(user) {

        const firstname =
            user.firstname || "";

        const lastname =
            user.lastname || "";

        const fullname =
            `${firstname} ${lastname}`.trim();


        sidebarProfileName.textContent =
            fullname || "ไม่ระบุชื่อ";


        if (
            user.year !== null &&
            user.year !== undefined
        ) {
            sidebarProfileYear.textContent =
                `ปี ${user.year}`;
        } else {
            sidebarProfileYear.textContent =
                "";
        }


        sidebarProfileDepartment.textContent =
            user.department || "";


        if (user.image_url) {
            sidebarProfileImage.src =
                user.image_url;
        }
    }


    // ========================================
    // กด Profile ใน Sidebar
    // ========================================

    profileButton.addEventListener(
        "click",
        (event) => {

            event.preventDefault();

            openProfileModal();
        }
    );


    // ========================================
    // ปุ่มต่าง ๆ
    // ========================================

    closeProfileModal.addEventListener(
        "click",
        closeModal
    );

    cancelProfileEdit.addEventListener(
        "click",
        closeModal
    );

    profileModalOverlay.addEventListener(
        "click",
        closeModal
    );

});