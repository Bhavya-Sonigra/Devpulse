"use client";

import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { CheckCircle2, ChevronRight, Check, Loader2 } from "lucide-react";
import { GithubIcon, SlackIcon } from "@/components/Icons";
import { useAppState } from "@/hooks/useAppState";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import { apiUrl } from "@/lib/api";

export default function SetupPage() {
  const { data: session } = useSession();
  const token = (session as any)?.backendToken;

  const [step, setStep] = useState(1);
  const [repos, setRepos] = useState<any[]>([]);
  const [channels, setChannels] = useState<any[]>([]);
  const [selectedRepo, setSelectedRepo] = useState<string | null>(null);
  const [selectedChannel, setSelectedChannel] = useState<string | null>(null);
  const [isSlackConnected, setIsSlackConnected] = useState(false);
  const [loadingRepos, setLoadingRepos] = useState(false);
  const [loadingChannels, setLoadingChannels] = useState(false);
  const [loadingAuth, setLoadingAuth] = useState(false);
  
  const { state, updateState, isLoaded } = useAppState();
  const router = useRouter();

  // Redirect if not first time and not explicitly forced
  useEffect(() => {
    if (isLoaded) {
      const isForce = window.location.search.includes("force=true");
      if (!state.isFirstTimeUser && !isForce) {
        router.push("/dashboard");
      }
      
      const stepMatch = window.location.search.match(/step=(\d)/);
      if (stepMatch && step === 1) {
        setStep(parseInt(stepMatch[1], 10));
      }
    }
  }, [isLoaded, state.isFirstTimeUser, router]);

  // Check if we came back from Slack OAuth (Slack connected)
  // We can just fetch channels to see if we have them
  useEffect(() => {
    if (token) {
      checkSlackConnection();
    }
  }, [token]);

  const checkSlackConnection = async () => {
    try {
      setLoadingChannels(true);
      const res = await fetch(apiUrl("/api/slack/channels"), {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        if (data && data.length > 0) {
          setChannels(data);
          setIsSlackConnected(true);
          // If we have channels, we likely returned from OAuth, let's jump to step 2
          setStep(2);
        }
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingChannels(false);
    }
  };

  const fetchRepos = async () => {
    try {
      setLoadingRepos(true);
      const res = await fetch(apiUrl("/api/repos/available"), {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        setRepos(data);
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingRepos(false);
    }
  };

  useEffect(() => {
    if (step === 1 && token && repos.length === 0) {
      fetchRepos();
    }
  }, [step, token]);

  const handleConnectSlack = async () => {
    try {
      setLoadingAuth(true);
      const res = await fetch(apiUrl("/api/slack/auth"), {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        if (data.url) {
          window.location.href = data.url;
        }
      }
    } catch (e) {
      console.error("Failed to connect slack", e);
      setLoadingAuth(false);
    }
  };

  const handleFinish = async () => {
    // Save to backend
    try {
      if (selectedRepo) {
        await fetch(apiUrl("/api/repos/connect"), {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify({ repoFullName: selectedRepo })
        });
      }
    } catch (e) {
      console.error("Failed to connect repo to backend", e);
    }

    updateState({
      isFirstTimeUser: false,
      connectedRepo: selectedRepo,
      connectedSlack: selectedChannel,
    });
    router.push("/dashboard");
  };

  const slideVariants = {
    hidden: { x: 50, opacity: 0 },
    visible: { x: 0, opacity: 1 },
    exit: { x: -50, opacity: 0 },
  };

  if (!isLoaded) return null;

  return (
    <main className="min-h-screen flex items-center justify-center bg-gray-50 p-6">
      <div className="w-full max-w-xl">
        {/* Progress Bar */}
        <div className="mb-8 flex items-center justify-between relative px-2">
          <div className="absolute left-0 top-1/2 -translate-y-1/2 w-full h-1 bg-gray-200 -z-10 rounded-full">
            <motion.div 
              className="h-full bg-indigo-600 rounded-full"
              initial={{ width: "0%" }}
              animate={{ width: `${((step - 1) / 2) * 100}%` }}
              transition={{ duration: 0.5 }}
            />
          </div>
          {[1, 2, 3].map((s) => (
            <div 
              key={s} 
              className={`w-10 h-10 rounded-full flex items-center justify-center font-semibold text-sm transition-colors duration-300 ${
                step >= s ? "bg-indigo-600 text-white shadow-md shadow-indigo-200" : "bg-white text-gray-400 border border-gray-200"
              }`}
            >
              {step > s ? <Check className="w-5 h-5" /> : s}
            </div>
          ))}
        </div>

        {/* Content Card */}
        <div className="bg-white rounded-3xl shadow-xl shadow-gray-200/50 border border-gray-100 overflow-hidden relative min-h-[550px] flex flex-col">
          <AnimatePresence mode="wait">
            {step === 1 && (
              <motion.div
                key="step1"
                variants={slideVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
                transition={{ duration: 0.3 }}
                className="p-10 flex flex-col flex-1"
              >
                <div className="flex-1">
                  <div className="w-12 h-12 bg-gray-100 rounded-2xl flex items-center justify-center mb-6 text-gray-900">
                    <GithubIcon className="w-6 h-6" />
                  </div>
                  <h2 className="text-2xl font-bold text-gray-900 mb-2">Connect Repository</h2>
                  <p className="text-gray-500 mb-6">Select the GitHub repository you want to track.</p>
                  
                  {loadingRepos ? (
                    <div className="flex justify-center py-8">
                      <Loader2 className="w-8 h-8 text-indigo-600 animate-spin" />
                    </div>
                  ) : (
                    <div className="space-y-3 max-h-[300px] overflow-y-auto pr-2">
                      {repos.map((repo: any) => (
                        <button
                          key={repo.id}
                          onClick={() => setSelectedRepo(repo.full_name)}
                          className={`w-full text-left p-4 rounded-xl border transition-all flex items-center justify-between ${
                            selectedRepo === repo.full_name 
                              ? "border-indigo-600 bg-indigo-50/50 ring-1 ring-indigo-600" 
                              : "border-gray-200 hover:border-gray-300 hover:bg-gray-50"
                          }`}
                        >
                          <span className="font-medium text-gray-900">{repo.full_name}</span>
                          {selectedRepo === repo.full_name && <CheckCircle2 className="w-5 h-5 text-indigo-600" />}
                        </button>
                      ))}
                      {repos.length === 0 && !loadingRepos && (
                        <p className="text-sm text-gray-500 text-center py-4">No repositories found.</p>
                      )}
                    </div>
                  )}
                </div>
                <button
                  onClick={() => setStep(2)}
                  disabled={!selectedRepo}
                  className="w-full mt-8 flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 disabled:hover:bg-indigo-600 text-white py-3.5 rounded-xl font-medium transition-all"
                >
                  Continue <ChevronRight className="w-5 h-5" />
                </button>
              </motion.div>
            )}

            {step === 2 && (
              <motion.div
                key="step2"
                variants={slideVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
                transition={{ duration: 0.3 }}
                className="p-10 flex flex-col flex-1"
              >
                <div className="flex-1">
                  <div className="w-12 h-12 bg-[#E01E5A]/10 rounded-2xl flex items-center justify-center mb-6 text-[#E01E5A]">
                    <SlackIcon className="w-6 h-6" />
                  </div>
                  <h2 className="text-2xl font-bold text-gray-900 mb-2">Connect Slack</h2>
                  <p className="text-gray-500 mb-6">Choose where you want to receive reports and alerts.</p>
                  
                  {!isSlackConnected ? (
                    <button
                      onClick={handleConnectSlack}
                      disabled={loadingAuth}
                      className="w-full flex items-center justify-center gap-3 bg-white border border-gray-200 hover:bg-gray-50 disabled:opacity-50 text-gray-900 py-4 px-4 rounded-xl font-medium transition-all shadow-sm"
                    >
                      {loadingAuth ? <Loader2 className="w-5 h-5 animate-spin" /> : <SlackIcon className="w-5 h-5 text-[#E01E5A]" />}
                      Connect Slack Workspace
                    </button>
                  ) : (
                    <div className="space-y-4 animate-in fade-in slide-in-from-bottom-4 duration-500">
                      <div className="flex items-center gap-3 text-sm text-emerald-600 bg-emerald-50 p-3 rounded-lg border border-emerald-100">
                        <CheckCircle2 className="w-5 h-5" />
                        Workspace connected successfully
                      </div>
                      
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">Select Channel</label>
                        {loadingChannels ? (
                           <div className="flex justify-center py-4">
                             <Loader2 className="w-6 h-6 text-indigo-600 animate-spin" />
                           </div>
                        ) : (
                          <div className="space-y-2 max-h-[250px] overflow-y-auto pr-2">
                            {channels.map((channel: any) => (
                              <button
                                key={channel.id}
                                onClick={() => setSelectedChannel(channel.name)}
                                className={`w-full text-left p-3.5 rounded-xl border transition-all flex items-center justify-between ${
                                  selectedChannel === channel.name 
                                    ? "border-indigo-600 bg-indigo-50/50" 
                                    : "border-gray-200 hover:border-gray-300"
                                }`}
                              >
                                <span className="font-medium text-gray-900">#{channel.name}</span>
                                {selectedChannel === channel.name && <CheckCircle2 className="w-5 h-5 text-indigo-600" />}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
                <div className="flex gap-3 mt-8">
                  <button
                    onClick={() => setStep(1)}
                    className="flex-1 bg-white border border-gray-200 hover:bg-gray-50 text-gray-900 py-3.5 rounded-xl font-medium transition-all"
                  >
                    Back
                  </button>
                  <button
                    onClick={() => setStep(3)}
                    disabled={!selectedChannel}
                    className="flex-[2] flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 disabled:hover:bg-indigo-600 text-white py-3.5 rounded-xl font-medium transition-all"
                  >
                    Continue <ChevronRight className="w-5 h-5" />
                  </button>
                </div>
              </motion.div>
            )}

            {step === 3 && (
              <motion.div
                key="step3"
                variants={slideVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
                transition={{ duration: 0.3 }}
                className="p-10 flex flex-col flex-1 items-center justify-center text-center"
              >
                <div className="w-20 h-20 bg-emerald-100 rounded-full flex items-center justify-center mb-6">
                  <CheckCircle2 className="w-10 h-10 text-emerald-600" />
                </div>
                <h2 className="text-3xl font-bold text-gray-900 mb-2">You're All Set!</h2>
                <p className="text-gray-500 mb-8 max-w-[280px]">
                  Your repository and Slack channel are connected. Let's look at your dashboard.
                </p>
                
                <div className="w-full bg-gray-50 rounded-2xl p-4 border border-gray-100 space-y-3 mb-8 text-sm text-left">
                  <div className="flex items-center justify-between">
                    <span className="text-gray-500">Repository</span>
                    <span className="font-medium text-gray-900 flex items-center gap-1.5"><GithubIcon className="w-4 h-4"/> {selectedRepo}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-gray-500">Slack Channel</span>
                    <span className="font-medium text-gray-900 flex items-center gap-1.5"><SlackIcon className="w-4 h-4 text-[#E01E5A]"/> #{selectedChannel}</span>
                  </div>
                </div>

                <button
                  onClick={handleFinish}
                  className="w-full bg-gray-900 hover:bg-gray-800 text-white py-4 rounded-xl font-medium transition-all shadow-md hover:shadow-lg hover:-translate-y-0.5"
                >
                  Go to Dashboard
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </main>
  );
}
