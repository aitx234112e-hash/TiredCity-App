export interface Account {
  _id?: string;
  profileName: string;
  fullName?: string;
  email: string;
  phone: string;
  gender: 'male' | 'female' | 'other';
  birthDay?: number;
  birthMonth?: number;
  birthYear?: number;
  avatar?: string;
  role?: 'user' | 'admin';
}