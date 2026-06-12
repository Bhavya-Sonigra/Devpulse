"use client";

import { useSession, signIn, signOut } from "next-auth/react";

export function AuthStatus() {
  const { data: session, status } = useSession();

  if (status === "loading") {
    return <div className="text-sm text-gray-500">Loading auth status...</div>;
  }

  if (session) {
    return (
      <div className="flex items-center gap-4">
        <p className="text-sm text-gray-700">
          Signed in as <span className="font-medium text-gray-900">{session.user?.name || session.user?.email}</span>
        </p>
        <button
          onClick={() => signOut()}
          className="rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 transition-colors"
        >
          Sign out
        </button>
      </div>
    );
  }

  return (
    <div className="flex items-center gap-3">
      <button
        onClick={() => signIn("github")}
        className="rounded-md bg-gray-900 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-gray-800 transition-colors"
      >
        Sign in with GitHub
      </button>
      <button
        onClick={() => signIn("slack")}
        className="rounded-md bg-[#4A154B] px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-[#3d113e] transition-colors"
      >
        Sign in with Slack
      </button>
    </div>
  );
}
