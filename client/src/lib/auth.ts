import type { NextAuthOptions } from "next-auth";
import GithubProvider from "next-auth/providers/github";
import SlackProvider from "next-auth/providers/slack";
import { apiUrl } from "@/lib/api";

export const authOptions: NextAuthOptions = {
  providers: [
    GithubProvider({
      clientId: process.env.GITHUB_ID as string,
      clientSecret: process.env.GITHUB_SECRET as string,
      authorization: { params: { scope: "read:user user:email repo admin:repo_hook" } },
    }),
    SlackProvider({
      clientId: process.env.SLACK_CLIENT_ID as string,
      clientSecret: process.env.SLACK_CLIENT_SECRET as string,
    }),
  ],
  secret: process.env.NEXTAUTH_SECRET,
  session: {
    strategy: "jwt",
  },
  pages: {
    signIn: '/login',
  },
  callbacks: {
    async jwt({ token, account, profile }) {
      if (account && profile) {
        try {
          const githubProfile = profile as any;
          const res = await fetch(apiUrl("/api/auth/github"), {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              githubId: githubProfile.id,
              login: githubProfile.login,
              accessToken: account.access_token,
              email: githubProfile.email
            })
          });
          if (res.ok) {
            const data = await res.json();
            token.backendToken = data.token;
            token.teamId = data.teamId;
          }
        } catch (e) {
          console.error("Failed to authenticate with backend", e);
        }
      }
      return token;
    },
    async session({ session, token }) {
      if (session.user && token.sub) {
        (session.user as any).id = token.sub;
      }
      (session as any).backendToken = token.backendToken;
      (session as any).teamId = token.teamId;
      return session;
    },
  },
};
