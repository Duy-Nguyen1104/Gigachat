import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { useAuth } from "../context/AuthContext";

interface LoginForm {
  email: string;
  password: string;
}

/* ── decorative SVG illustrations ── */

function ChatBubbleIllustration() {
  return (
    <svg
      width="280"
      height="220"
      viewBox="0 0 280 220"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className="drop-shadow-lg"
    >
      {/* Main chat card */}
      <rect x="30" y="10" width="220" height="160" rx="20" fill="white" />
      <rect
        x="30"
        y="10"
        width="220"
        height="160"
        rx="20"
        stroke="#e5e7eb"
        strokeWidth="1"
      />
      {/* Avatar */}
      <circle cx="70" cy="60" r="18" fill="#f9ef4a" />
      <circle cx="70" cy="54" r="7" fill="#a89c0d" />
      <ellipse cx="70" cy="68" rx="10" ry="6" fill="#a89c0d" />
      {/* Chat lines */}
      <rect x="100" y="48" width="120" height="10" rx="5" fill="#f3f4f6" />
      <rect x="100" y="66" width="90" height="10" rx="5" fill="#f3f4f6" />
      {/* Second message */}
      <circle cx="70" cy="110" r="18" fill="#d4c510" />
      <circle cx="70" cy="104" r="7" fill="#7c730a" />
      <ellipse cx="70" cy="118" rx="10" ry="6" fill="#7c730a" />
      <rect x="100" y="98" width="110" height="10" rx="5" fill="#f3f4f6" />
      <rect x="100" y="116" width="80" height="10" rx="5" fill="#f3f4f6" />
      {/* Typing indicator */}
      <rect x="50" y="145" width="60" height="16" rx="8" fill="#fdfce8" />
      <circle cx="67" cy="153" r="3" fill="#d4c510" />
      <circle cx="79" cy="153" r="3" fill="#f7e812" />
      <circle cx="91" cy="153" r="3" fill="#f9ef4a" />
    </svg>
  );
}

function EmojiReaction() {
  return (
    <svg
      width="56"
      height="56"
      viewBox="0 0 56 56"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className="drop-shadow-md"
    >
      <circle cx="28" cy="28" r="28" fill="#f9ef4a" />
      <circle cx="20" cy="22" r="3" fill="#7c730a" />
      <circle cx="36" cy="22" r="3" fill="#7c730a" />
      <path
        d="M18 34 C22 40, 34 40, 38 34"
        stroke="#7c730a"
        strokeWidth="2.5"
        strokeLinecap="round"
        fill="none"
      />
    </svg>
  );
}

function HeartBadge() {
  return (
    <svg
      width="44"
      height="44"
      viewBox="0 0 44 44"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className="drop-shadow-md"
    >
      <circle cx="22" cy="22" r="22" fill="#f7e812" />
      <path
        d="M22 32 C18 28, 12 24, 12 19 C12 15, 15 13, 18 13 C20 13, 21.5 14, 22 15 C22.5 14, 24 13, 26 13 C29 13, 32 15, 32 19 C32 24, 26 28, 22 32Z"
        fill="white"
      />
    </svg>
  );
}

function GroupAvatars() {
  return (
    <svg
      width="160"
      height="70"
      viewBox="0 0 160 70"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className="drop-shadow-md"
    >
      {/* Person 1 */}
      <circle
        cx="35"
        cy="35"
        r="30"
        fill="white"
        stroke="#e5e7eb"
        strokeWidth="2"
      />
      <circle cx="35" cy="28" r="10" fill="#f9ef4a" />
      <ellipse cx="35" cy="48" rx="14" ry="9" fill="#f9ef4a" />
      {/* Person 2 */}
      <circle
        cx="80"
        cy="35"
        r="30"
        fill="white"
        stroke="#e5e7eb"
        strokeWidth="2"
      />
      <circle cx="80" cy="28" r="10" fill="#d4c510" />
      <ellipse cx="80" cy="48" rx="14" ry="9" fill="#d4c510" />
      {/* Person 3 */}
      <circle
        cx="125"
        cy="35"
        r="30"
        fill="white"
        stroke="#e5e7eb"
        strokeWidth="2"
      />
      <circle cx="125" cy="28" r="10" fill="#a89c0d" />
      <ellipse cx="125" cy="48" rx="14" ry="9" fill="#a89c0d" />
    </svg>
  );
}

export default function LoginPage() {
  const [error, setError] = useState("");
  const {
    register,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm<LoginForm>();
  const { login } = useAuth();
  const navigate = useNavigate();

  const onSubmit = async (data: LoginForm) => {
    setError("");
    try {
      await login(data.email, data.password);
      navigate("/");
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setError(error.response?.data?.message || "Invalid credentials");
    }
  };

  return (
    <div className="min-h-screen flex flex-col lg:flex-row bg-white">
      {/* ── Left hero panel ── */}
      <div className="hidden lg:flex lg:w-1/2 flex-col justify-center items-center px-12 xl:px-20 bg-gradient-to-br from-brand-50 via-white to-brand-100 relative overflow-hidden">
        {/* Decorative blurred blobs */}
        <div className="absolute -top-32 -left-32 w-96 h-96 bg-brand-200/40 rounded-full blur-3xl" />
        <div className="absolute bottom-0 right-0 w-80 h-80 bg-brand-300/30 rounded-full blur-3xl" />

        <div className="relative z-10 max-w-lg flex flex-col items-start gap-10">
          {/* Brand name */}
          <div className="flex items-center gap-3">
            <svg
              width="44"
              height="44"
              viewBox="0 0 44 44"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <rect width="44" height="44" rx="12" fill="#f9ef4a" />
              <path
                d="M12 22 C12 16, 16 12, 22 12 C28 12, 32 16, 32 22 C32 26, 30 29, 27 31 L28 36 L23 32 C22.7 32 22.3 32, 22 32 C16 32, 12 28, 12 22Z"
                fill="white"
              />
            </svg>
            <span className="text-2xl font-bold text-gray-900 tracking-tight">
              GigaChat
            </span>
          </div>

          {/* Tagline */}
          <h2 className="text-5xl xl:text-6xl font-extrabold leading-tight text-gray-900">
            Connect&nbsp;with
            <br />
            the&nbsp;people
            <br />
            <span className="text-brand-600">you love.</span>
          </h2>

          {/* Illustrations collage */}
          <div className="relative w-full h-64 mt-2">
            {/* Main chat card */}
            <div className="absolute top-0 left-0">
              <ChatBubbleIllustration />
            </div>
            {/* Emoji floating top-right */}
            <div
              className="absolute -top-4 right-16 animate-bounce"
              style={{ animationDuration: "3s" }}
            >
              <EmojiReaction />
            </div>
            {/* Heart badge bottom-right */}
            <div
              className="absolute bottom-8 right-8 animate-bounce"
              style={{ animationDuration: "4s", animationDelay: "1s" }}
            >
              <HeartBadge />
            </div>
            {/* Group avatars bottom */}
            <div className="absolute -bottom-2 left-12">
              <GroupAvatars />
            </div>
          </div>
        </div>
      </div>

      {/* ── Right login panel ── */}
      <div className="flex-1 flex flex-col items-center justify-center px-6 py-12 lg:px-16">
        {/* Mobile-only brand header */}
        <div className="lg:hidden flex flex-col items-center mb-8">
          <svg
            width="48"
            height="48"
            viewBox="0 0 44 44"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <rect width="44" height="44" rx="12" fill="#f9ef4a" />
            <path
              d="M12 22 C12 16, 16 12, 22 12 C28 12, 32 16, 32 22 C32 26, 30 29, 27 31 L28 36 L23 32 C22.7 32 22.3 32, 22 32 C16 32, 12 28, 12 22Z"
              fill="white"
            />
          </svg>
          <h1 className="mt-3 text-2xl font-bold text-gray-900">GigaChat</h1>
        </div>

        <div className="w-full max-w-md">
          <h3 className="text-xl font-semibold text-gray-900 mb-6 text-center">
            Log in to GigaChat
          </h3>

          {/* Form Card */}
          <div className="bg-white rounded-2xl shadow-lg shadow-gray-200/50 border border-gray-100 p-8">
            {error && (
              <div className="mb-5 px-4 py-3 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email address
                </label>
                <input
                  type="email"
                  {...register("email", { required: true })}
                  placeholder="you@example.com"
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-400 focus:border-transparent transition text-sm"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Password
                </label>
                <input
                  type="password"
                  {...register("password", { required: true })}
                  placeholder="••••••••"
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-400 focus:border-transparent transition text-sm"
                />
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full mt-2 px-4 py-3.5 bg-brand-400 hover:bg-brand-500 text-gray-900 font-semibold rounded-xl transition disabled:opacity-50 disabled:cursor-not-allowed shadow-sm hover:shadow-md text-sm"
              >
                {isSubmitting ? "Signing in..." : "Log in"}
              </button>
            </form>

            <div className="mt-5 flex items-center justify-center">
              <hr className="flex-1 border-gray-200" />
              <span className="px-4 text-xs text-gray-400 uppercase tracking-wide">
                or
              </span>
              <hr className="flex-1 border-gray-200" />
            </div>

            <Link
              to="/signup"
              className="mt-5 block w-full text-center px-4 py-3 border-2 border-brand-400 text-brand-700 font-semibold rounded-xl hover:bg-brand-50 transition text-sm"
            >
              Create new account
            </Link>
          </div>

          <p className="mt-6 text-center text-xs text-gray-400">
            &copy; {new Date().getFullYear()} GigaChat
          </p>
        </div>
      </div>
    </div>
  );
}
