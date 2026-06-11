export interface MeResponse {
  user: {
    id: string;
    email: string;
    role: string;
  };
  organization: {
    id: string;
    name: string;
    logoUrl: string;
    brandColor: string;
    plan: string;
  };
}
