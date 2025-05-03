const startBtn = document.getElementById("start");
const urlInput = document.getElementById("url");
const statusTxt = document.getElementById("status");
const spinner = document.getElementById("spinner");
const results = document.getElementById("results");
const onlyLogos = document.getElementById("onlyLogos");
let evtSource;

let totalCount = 0;
let logoCount = 0;

startBtn.addEventListener("click", () => {
  if (evtSource) evtSource.close();
  results.innerHTML = "";
  totalCount = 0;
  logoCount = 0;
  statusTxt.className = "busy";
  statusTxt.textContent = "🔄 Connecting…";
  spinner.style.display = "block";

  const raw = urlInput.value.trim();
  if (!raw) {
    statusTxt.className = "error";
    statusTxt.textContent = "⚠️ Please enter a valid URL.";
    spinner.style.display = "none";
    return;
  }

  const url = encodeURIComponent(raw);
  evtSource = new EventSource(`/stream?url=${url}`);
  let firstImage = true;

  evtSource.onmessage = (evt) => {
    if (firstImage) {
      spinner.style.display = "none";
      statusTxt.textContent = "";
      firstImage = false;
    }

    const item = JSON.parse(evt.data);
    totalCount++;
    if (item.likelyLogo) logoCount++;

    if (onlyLogos.checked) {
      statusTxt.className = "busy";
      statusTxt.textContent = `🔍 Showing ${logoCount} logo${logoCount === 1 ? "" : "s"}…`;
    } else {
      statusTxt.className = "busy";
      statusTxt.textContent = `🔍 Found ${totalCount} image${totalCount === 1 ? "" : "s"}…`;
    }

    if (onlyLogos.checked && !item.likelyLogo) return;

    const li = document.createElement("li");
    if (item.likelyLogo) li.classList.add("logo");

    const img = document.createElement("img");
    img.src = item.url;
    img.alt = item.likelyLogo ? "Likely logo" : "Image";

    const overlay = document.createElement("div");
    overlay.className = "overlay";
    const filename = item.url.split("/").pop().split("?")[0];
    overlay.innerHTML = `
      <div>${filename}</div>
      <a href="${item.url}" download>Download</a>`;

    li.append(img, overlay);
    results.appendChild(li);
  };

  evtSource.addEventListener("done", () => {
    spinner.style.display = "none";
    if (firstImage) {
      statusTxt.className = "error";
      statusTxt.textContent = "😕 No images found.";
    } else if (onlyLogos.checked) {
      statusTxt.className = "success";
      statusTxt.textContent = `✅ Crawl complete — ${logoCount} logo${logoCount === 1 ? "" : "s"} shown.`;
    } else {
      statusTxt.className = "success";
      statusTxt.textContent = `✅ Crawl complete — ${totalCount} image${totalCount === 1 ? "" : "s"} found.`;
    }
    evtSource.close();
  });

  evtSource.onerror = (err) => {
    spinner.style.display = "none";
    statusTxt.className = "error";
    statusTxt.textContent = "❌ Error during crawl.";
    console.error("SSE error:", err);
    evtSource.close();
  };
});

onlyLogos.addEventListener("change", () => {
  document.querySelectorAll("#results li").forEach((li) => {
    if (!li.classList.contains("logo")) {
      li.style.display = onlyLogos.checked ? "none" : "";
    }
  });

  if (totalCount > 0) {
    if (onlyLogos.checked) {
      statusTxt.className = "busy";
      statusTxt.textContent = `🔍 Showing ${logoCount} logo${logoCount === 1 ? "" : "s"}…`;
    } else {
      statusTxt.className = "busy";
      statusTxt.textContent = `🔍 Found ${totalCount} image${totalCount === 1 ? "" : "s"}…`;
    }
  }
});
