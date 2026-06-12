"use client";

import { Navbar } from "@/components/Navbar";
import { 
  GitCommit, 
  GitPullRequest, 
  Trophy, 
  TrendingUp, 
  BellRing,
  Activity,
  MessageSquare,
  Loader2
} from "lucide-react";
import { 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  Area,
  AreaChart
} from "recharts";
import { useState, useEffect } from "react";
import { useSession } from "next-auth/react";
import { apiUrl } from "@/lib/api";

export default function DashboardPage() {
  const { data: session } = useSession();
  const token = (session as any)?.backendToken;

  const [isTriggering, setIsTriggering] = useState(false);
  
  const [latestMetrics, setLatestMetrics] = useState<any>(null);
  const [history, setHistory] = useState<any[]>([]);
  const [deliveries, setDeliveries] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (token) {
      fetchDashboardData();
    }
  }, [token]);

  const fetchDashboardData = async () => {
    try {
      setIsLoading(true);
      
      if (!token) {
        console.warn("No authentication token available");
        setIsLoading(false);
        return;
      }
      
      const headers = { Authorization: `Bearer ${token}` };

      const [latestRes, historyRes, deliveriesRes] = await Promise.all([
        fetch(apiUrl("/api/metrics/latest"), { headers })
          .catch((err) => { console.error("Latest metrics fetch failed:", err); return null; }),
        fetch(apiUrl("/api/metrics/history"), { headers })
          .catch((err) => { console.error("History metrics fetch failed:", err); return null; }),
        fetch(apiUrl("/api/deliveries"), { headers })
          .catch((err) => { console.error("Deliveries fetch failed:", err); return null; })
      ]);

      // Handle latest metrics (204 means no content)
      if (latestRes) {
        if (latestRes.status === 204) {
          console.info("No metrics data yet for this team");
          setLatestMetrics(null);
        } else if (latestRes.ok) {
          const data = await latestRes.json();
          setLatestMetrics(data);
          console.log("Latest metrics loaded:", data);
        } else {
          console.error("Latest metrics error:", latestRes.status, await latestRes.text());
        }
      }

      // Handle history
      if (historyRes) {
        if (historyRes.status === 204) {
          console.info("No history data yet");
          setHistory([]);
        } else if (historyRes.ok) {
          const data = await historyRes.json();
          setHistory(Array.isArray(data) ? data : []);
          console.log("History loaded with", data?.length || 0, "records");
        } else {
          console.error("History error:", historyRes.status, await historyRes.text());
        }
      }

      // Handle deliveries
      if (deliveriesRes) {
        if (deliveriesRes.status === 204) {
          console.info("No delivery logs yet");
          setDeliveries([]);
        } else if (deliveriesRes.ok) {
          const data = await deliveriesRes.json();
          setDeliveries(Array.isArray(data) ? data : []);
          console.log("Deliveries loaded with", data?.length || 0, "records");
        } else {
          console.error("Deliveries error:", deliveriesRes.status, await deliveriesRes.text());
        }
      }
      
    } catch (e) {
      console.error("Failed to fetch dashboard data", e);
    } finally {
      setIsLoading(false);
    }
  };

  const handleTrigger = async () => {
    if (!token) {
      console.warn("No token available for trigger");
      return;
    }
    
    setIsTriggering(true);
    try {
      const response = await fetch(apiUrl("/api/report/trigger"), {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json"
        }
      });
      
      if (response.ok) {
        console.log("Report triggered successfully");
        // Wait a moment then re-fetch to see the new delivery
        setTimeout(() => fetchDashboardData(), 1000);
      } else {
        const errorText = await response.text();
        console.error("Failed to trigger report:", response.status, errorText);
      }
    } catch (e) {
      console.error("Failed to trigger report", e);
    } finally {
      setIsTriggering(false);
    }
  };

  // Format chart data
  const chartData = history.map(h => ({
    week: new Date(h.weekStart).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
    velocity: h.prsMerged || 0
  })).reverse(); // Assuming history might be newest first, we want chronological order for chart

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <main className="max-w-6xl mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
            <p className="text-gray-500">Welcome back. Here's your team's velocity this week.</p>
          </div>
          <button
            onClick={handleTrigger}
            disabled={isTriggering || !token}
            className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-70 text-white px-4 py-2.5 rounded-xl font-medium transition-all shadow-sm"
          >
            {isTriggering ? <Activity className="w-4 h-4 animate-spin" /> : <BellRing className="w-4 h-4" />}
            {isTriggering ? "Sending..." : "Trigger Report"}
          </button>
        </div>

        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-20">
             <Loader2 className="w-10 h-10 text-indigo-600 animate-spin mb-4" />
             <p className="text-gray-500">Loading your data...</p>
          </div>
        ) : (
          <>
            {/* Metrics Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
              <MetricCard 
                title="Commits" 
                value={latestMetrics?.totalCommits || 0} 
                trend="This Week" 
                isPositive={true}
                icon={GitCommit} 
                color="text-blue-600"
                bg="bg-blue-100"
              />
              <MetricCard 
                title="Pull Requests" 
                value={latestMetrics?.prsOpened || 0} 
                trend={`${latestMetrics?.prsMerged || 0} merged`} 
                isPositive={true}
                icon={GitPullRequest} 
                color="text-emerald-600"
                bg="bg-emerald-100"
              />
              <MetricCard 
                title="Top Contributor" 
                value={latestMetrics?.topContributor || "N/A"} 
                trend="MVP" 
                isPositive={true}
                icon={Trophy} 
                color="text-amber-600"
                bg="bg-amber-100"
              />
              <MetricCard 
                title="Velocity Trend" 
                value={latestMetrics?.prsMerged > 0 ? "Active" : "Quiet"} 
                trend="Based on merges" 
                isPositive={latestMetrics?.prsMerged > 0}
                icon={TrendingUp} 
                color="text-purple-600"
                bg="bg-purple-100"
              />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* Chart Section */}
              <div className="lg:col-span-2 bg-white rounded-2xl border border-gray-200 p-6 shadow-sm flex flex-col">
                <div className="mb-6">
                  <h2 className="text-lg font-semibold text-gray-900">Velocity History</h2>
                  <p className="text-sm text-gray-500">Number of PRs merged over time.</p>
                </div>
                <div className="flex-1 w-full" style={{ minWidth: 0, minHeight: 300 }}>
                  {chartData.length > 0 ? (
                    <ResponsiveContainer width="100%" height={300}>
                      <AreaChart data={chartData}>
                        <defs>
                          <linearGradient id="colorVelocity" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="#4f46e5" stopOpacity={0.3}/>
                            <stop offset="95%" stopColor="#4f46e5" stopOpacity={0}/>
                          </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f3f4f6" />
                        <XAxis 
                          dataKey="week" 
                          axisLine={false} 
                          tickLine={false} 
                          tick={{ fill: '#9ca3af', fontSize: 12 }} 
                          dy={10}
                        />
                        <YAxis 
                          axisLine={false} 
                          tickLine={false} 
                          tick={{ fill: '#9ca3af', fontSize: 12 }}
                          allowDecimals={false}
                        />
                        <Tooltip 
                          contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                        />
                        <Area 
                          type="monotone" 
                          dataKey="velocity" 
                          stroke="#4f46e5" 
                          strokeWidth={3}
                          fillOpacity={1} 
                          fill="url(#colorVelocity)" 
                        />
                      </AreaChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="h-full flex items-center justify-center text-gray-400">
                      Not enough data yet.
                    </div>
                  )}
                </div>
              </div>

              {/* Slack Activity Section */}
              <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm flex flex-col">
                <div className="mb-6">
                  <h2 className="text-lg font-semibold text-gray-900">Recent Slack Reports</h2>
                  <p className="text-sm text-gray-500">Latest automated updates.</p>
                </div>
                
                <div className="flex-1 flex flex-col gap-4 overflow-y-auto max-h-[350px] pr-2">
                  {deliveries.length > 0 ? deliveries.map((report) => (
                    <div key={report.id} className="p-4 rounded-xl bg-gray-50 border border-gray-100 hover:bg-gray-100 transition-colors">
                      <div className="flex items-start gap-3">
                        <div className={`mt-1 w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${
                          report.status === 'SUCCESS' ? 'bg-[#E01E5A]/10 text-[#E01E5A]' : 'bg-red-100 text-red-600'
                        }`}>
                          <MessageSquare className="w-4 h-4" />
                        </div>
                        <div>
                          <h4 className="text-sm font-semibold text-gray-900">
                            {report.status === 'SUCCESS' ? 'Report Delivered' : 'Delivery Failed'}
                          </h4>
                          <p className="text-xs text-gray-500 mt-1">
                            {new Date(report.deliveredAt).toLocaleString(undefined, {
                               month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit'
                            })}
                          </p>
                          <div className="mt-2 inline-block px-2 py-1 bg-white border border-gray-200 rounded-md text-xs font-medium text-gray-700">
                            {report.status === 'SUCCESS' ? report.channel || 'Slack' : 'Error'}
                          </div>
                        </div>
                      </div>
                    </div>
                  )) : (
                    <div className="h-full flex items-center justify-center text-gray-400 mt-10">
                      No reports triggered yet.
                    </div>
                  )}
                </div>
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
}

function MetricCard({ title, value, trend, isPositive, icon: Icon, color, bg }: any) {
  return (
    <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500">{title}</p>
          <h3 className="text-3xl font-bold text-gray-900 mt-2">{value}</h3>
        </div>
        <div className={`w-10 h-10 rounded-xl ${bg} flex items-center justify-center`}>
          <Icon className={`w-5 h-5 ${color}`} />
        </div>
      </div>
      <div className="mt-4 flex items-center gap-1.5">
        <span className={`text-sm font-medium ${isPositive ? 'text-emerald-600' : 'text-rose-600'}`}>
          {trend}
        </span>
      </div>
    </div>
  );
}
