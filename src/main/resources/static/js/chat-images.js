window.CPChatImages = {
  async prepare(file) {
    if (
      !["image/jpeg", "image/png", "image/webp"].includes(file.type) ||
      file.size > 8 * 1024 * 1024
    )
      throw new Error("เลือกรูป JPG, PNG หรือ WebP ขนาดไม่เกิน 8 MB");
    const url = URL.createObjectURL(file);
    try {
      const image = new Image();
      image.src = url;
      await image.decode();
      const ratio = Math.min(
        1,
        1600 / Math.max(image.naturalWidth, image.naturalHeight),
      );
      const canvas = document.createElement("canvas");
      canvas.width = Math.max(1, Math.round(image.naturalWidth * ratio));
      canvas.height = Math.max(1, Math.round(image.naturalHeight * ratio));
      const ctx = canvas.getContext("2d");
      ctx.fillStyle = "#fff";
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.drawImage(image, 0, 0, canvas.width, canvas.height);
      const data = canvas.toDataURL("image/jpeg", 0.82);
      if (data.length > 2800000)
        throw new Error("รูปมีขนาดใหญ่เกินไป กรุณาเลือกรูปที่เล็กลง");
      return data;
    } finally {
      URL.revokeObjectURL(url);
    }
  },
  async send(roomId, file) {
    const content = await this.prepare(file);
    const response = await fetch(
      `/api/chats/${encodeURIComponent(roomId)}/messages`,
      {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify({ content, messageType: "IMAGE" }),
      },
    );
    if (!response.ok)
      throw new Error(`ส่งรูปไม่สำเร็จ (${response.status}) กรุณาลองอีกครั้ง`);
    return response.json();
  },
};
