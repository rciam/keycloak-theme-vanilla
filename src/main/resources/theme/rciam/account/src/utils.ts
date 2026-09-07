export  function formatDateToString(date: any) {
  // Ensure that the input is a valid Date object
  if (!(date instanceof Date) || isNaN(date.getTime())) {
    throw new Error("Invalid date format. Please provide a valid Date object.");
  }
  const month = date.toLocaleString("default", { month: "long" });
  const day = date.getDate();
  const year = date.getFullYear();

  return `${month} ${day}, ${year}`;
}

export function normalizeAccountConsoleUrl() {
  const accountPathMatch = window.location.pathname.match(
    /^(.*\/realms\/[^/]+\/account)(?:\/.*)?$/,
  );

  if (!accountPathMatch) {
    return;
  }

  const canonicalPath = `${accountPathMatch[1]}/`;
  const canonicalHash = window.location.hash || "#/";

  if (
    window.location.pathname !== canonicalPath ||
    window.location.hash !== canonicalHash
  ) {
    window.history.replaceState(
      window.history.state,
      "",
      `${canonicalPath}${window.location.search}${canonicalHash}`,
    );
  }
}