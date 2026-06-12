"use client";

import { signIn, useSession } from "next-auth/react";
import { Loader2 } from "lucide-react";
import { GithubIcon } from "@/components/Icons";
import { useRouter } from "next/navigation";
import { useAppState } from "@/hooks/useAppState";
import { useEffect } from "react";

export default function LoginPage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const { state, isLoaded } = useAppState();

  useEffect(() => {
    if (status === "authenticated" && isLoaded) {
      if (state.isFirstTimeUser) {
        router.push("/setup");
      } else {
        router.push("/dashboard");
      }
    }
  }, [status, isLoaded, state.isFirstTimeUser, router]);

  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-6 bg-gradient-to-br from-indigo-50 via-white to-purple-50">
      <div className="w-full max-w-md bg-white/80 backdrop-blur-xl rounded-3xl shadow-xl border border-white/20 p-10 text-center relative overflow-hidden">
        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-indigo-500 to-purple-500" />
        
        <div className="mx-auto w-16 h-16 bg-gradient-to-br from-indigo-600 to-purple-600 rounded-2xl flex items-center justify-center mb-8 shadow-lg shadow-indigo-200">
          <svg
            className="w-8 h-8 text-white"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M13 10V3L4 14h7v7l9-11h-7z"
            />
          </svg>
        </div>
        
        <h1 className="text-3xl font-bold tracking-tight text-gray-900 mb-3 font-sans">
          Welcome to DevPulse
        </h1>
        <p className="text-gray-500 mb-10 text-sm">
          Sign in to connect your tools and start tracking engineering metrics.
        </p>

        {status === "loading" ? (
          <div className="flex justify-center items-center py-3">
            <Loader2 className="w-6 h-6 animate-spin text-indigo-600" />
          </div>
        ) : (
          <button
            onClick={() => signIn("github")}
            className="w-full flex items-center justify-center gap-3 bg-gray-900 hover:bg-gray-800 text-white py-3.5 px-4 rounded-xl font-medium transition-all shadow-md hover:shadow-xl hover:-translate-y-0.5"
          >
            <GithubIcon className="w-5 h-5" />
            Continue with GitHub
          </button>
        )}

        <div className="mt-10 text-xs text-gray-400">
          <p>By signing in, you agree to our Terms of Service.</p>
        </div>
      </div>
    </main>
  );
}
